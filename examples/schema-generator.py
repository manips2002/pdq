#!/usr/bin/env python

from functools import reduce
import operator
from dataclasses import dataclass
from typing import Optional
import sqlparse as sp
import argparse as ap
from enum import Enum

datatype_to_java = {
    'INT8' : 'java.lang.Integer',
    'NUMBER' : 'java.lang.Integer',
    'NUMERIC' : 'java.lang.Double',
    'VARCHAR' : 'java.lang.String',
    "DATE" : 'java.util.Date'
    }

access_method_counter = 1

class IncludePKs(Enum):
    NO = 0
    AS_PK = 1
    AS_EGD = 2

@dataclass
class Table:
    name: str
    attrs: list[tuple[str]]
    pk: Optional[list[str]]

    def attrNames(self):
        return [ x[0] for x in self.attrs ]

    def pkToFD(self):
        if self.pk:
            return FD(self, self.pk, self.attrNames())
        return None

    def pkToXML(self):
        if self.pk:
            return "\n" + "\n".join([ f"             <primaryKey>{a}</primaryKey>" for a in self.pk ])
        else:
            return ""

    def toXML(self,addPK=False):
        global access_method_counter
        access_method_counter += 1
        accm = f'm{access_method_counter}'
        attrStr = listConcat([ "             " + attrToXML(x[0], x[1]) for x in self.attrs])
        pkStr = self.pkToXML()
        return f"""     <relation name="{self.name}">
{attrStr}{pkStr}
             <access-method name="{accm}"/>
     </relation>
"""

    def viewXML(self):
        attrStr = listConcat([ "             " + attrToXML(x[0], x[1]) for x in self.attrs])
        return f"""<view name="{'v' + self.name}">
{attrStr}
\t</view>
"""


@dataclass
class Counter:
    val: int = 0

    def incr(self):
        self.val += 1
        return self.val - 1

@dataclass
class FD:
    table: Table
    lhs: list[str]
    rhs: list[str]

    @staticmethod
    def genVar(a, lhsToVar, rhsToVar, c):
        if a in lhsToVar:
            return lhsToVar[a]
        if a in rhsToVar:
            return rhsToVar[a]
        return varFromInt(c.incr())

    def toEDG(self):
        c = Counter()
        lhsToVar = { a: varFromInt(c.incr()) for a in self.lhs }
        onlyRhs = list(filter(lambda x: x not in self.lhs, self.rhs))
        rhsToVarLeft = { a: varFromInt(c.incr()) for a in onlyRhs }
        rhsToVarRight = { a: varFromInt(c.incr()) for a in onlyRhs }
        print(lhsToVar, onlyRhs)

        fa = atom(self.table.name, [ FD.genVar(a, lhsToVar, rhsToVarLeft, c) for a in self.table.attrNames()])
        sa = atom(self.table.name, [ FD.genVar(a, lhsToVar, rhsToVarRight, c) for a in self.table.attrNames()])
        eqs = [ (rhsToVarLeft[a], rhsToVarRight[a]) for a in onlyRhs ]

        return EDG([fa,sa], eqs)

    def toXML(self):
        return self.toEDG().toXML()

@dataclass
class atom:
    name: str
    args: list[str]

    def toXML(self, indent=3):
        varStrs = listConcat([ '\t' + varToXML(arg) for arg in self.args])
        return listConcat([ indent * '\t' + s for s in  f"""<atom name="{self.name}">
{varStrs}
</atom>""".split("\n")])

@dataclass
class TGD:
    lhs: list[atom]
    rhs: list[atom]

    def toXML(self):
        return f"""<dependency type="TGD">
      <body>
{listConcat([ a.toXML() for a in self.lhs])}
      </body>
      <head>
{listConcat([ a.toXML() for a in self.rhs])}
      </head>
    </dependency>
"""

@dataclass
class EDG:
    lhs: list[atom]
    rhs: list[tuple[str,str]]

    @staticmethod
    def equalityToXML(eq):
        return f"""
            <atom name="EQUALITY">
                <variable name="{eq[0]}" />
                <variable name="{eq[1]}" />
            </atom>
"""

    def toXML(self):
        return f"""    <dependency>
	  <body>
{listConcat([ a.toXML() for a in self.lhs])}
	  </body>
	  <head>
{listConcat([ EDG.equalityToXML(e) for e in self.rhs])}
	  </head>
    </dependency>
"""

def dtToJava(dt):
    global datatype_to_java
    return datatype_to_java[dt]

def attrToXML(name, dt):
    return f'<attribute name="{name}" type="{dtToJava(dt)}"/>'

def varFromInt(i, basename='x'):
    return f"x{i}"

def varToXML(name):
    return f'<variable name="{name}" />'

def createVarList(attrs, tabs):
    return listConcat([ (tabs * '\t') + varToXML(varFromInt(i)) for i in range(0,len(attrs)) ])

def createTableViewDep(table):
    return f"""
    <dependency type="TGD">
      <body>
        <atom name="{table.name}">
{createVarList(table.attrs, 3)}
        </atom>
      </body>
      <head>
        <atom name="{'v' + table.name}">
{createVarList(table.attrs, 3)}
        </atom>
      </head>
    </dependency>
    <dependency type="TGD">
      <body>
        <atom name="{'v' + table.name}">
{createVarList(table.attrs, 3)}
        </atom>
      </body>
      <head>
        <atom name="{table.name}">
{createVarList(table.attrs, 3)}
        </atom>
      </head>
    </dependency>
"""

def listConcat(strl, delim='\n'):
    return delim.join(strl)

def depToXML(d):
    return d.toXML()

def schemaToXML(tables, deps, pk=IncludePKs.NO):
    print(pk)
    strtable = [ t.toXML(pk is IncludePKs.AS_PK) for t in tables ]
    strviews = [ t.viewXML() for t in tables ]
    strdeps = [ createTableViewDep(t) for t in tables ]
    if pk is IncludePKs.AS_EGD:
        strdeps += [ depToXML(t.pkToFD().toEDG()) for t in tables if t.pk ]
    strdeps += [ depToXML(d) for d in deps ]

    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<schema>
  <relations>
    {listConcat(strtable)}
    {listConcat(strviews)}
  </relations>
  <dependencies>
    {listConcat(strdeps)}
  </dependencies>
</schema>
"""

def firstToken(lst, cls):
    return next(iter([ t for t in lst if  isinstance(t, cls)]))

def splitOnComma(ts):
    elements = []
    cur = []
    for t in ts:
        if t.ttype == sp.tokens.Punctuation and t.value == ',':
            elements.append(cur)
            cur = []
        else:
            cur.append(t)
    elements.append(cur)
    return elements

def extractAttrs(ts):
    attrs = []
    for el in ts:
        if isinstance(el[0], sp.sql.Identifier):
            attrs.append((el[0].value, el[1].value))
# TODO check for primary key
    return attrs

def sqlkeyToKey(tok):
    if isinstance(tok, sp.sql.Identifier):
        return [ tok.value ]
    else:
        return [ t.value for t in tok.get_identifiers() ]

def extractPK(ts):
    for el in ts:
        if el[0].value.lower() == 'primary' and el[1].value.lower() == 'key':
            els = splitOnComma([ t for t in firstToken(el, sp.sql.Parenthesis) if not t.is_whitespace ][1:-1])
            print(els)
            return sqlkeyToKey(els[0][0])
            return [ e[0].value for e in els ]
    return None

def sqlCreateTableParseToTable(st):
    nonwhite = [ t for t in st.tokens if not t.is_whitespace ]
    tableName = firstToken(nonwhite, sp.sql.Identifier).value
    els = [ t for t in firstToken(nonwhite, sp.sql.Parenthesis) if not t.is_whitespace ][1:-1]
    els = splitOnComma(els)
    attrs = extractAttrs(els)
    pk = extractPK(els)

    return Table(tableName, attrs, pk)

def sqlToSchema(f):
    tables = [ ]
    with open (f, "r") as sqlfile:
        content = "\n".join(sqlfile.readlines())
    parse = sp.parse(content)
    for st in parse:
        if st.get_type() == 'CREATE':
            table = sqlCreateTableParseToTable(st)
            tables.append(table)
    return tables

def writeXMLForSchema(sch, f, dep=[], pk=IncludePKs.NO):
    with open (f, 'w') as xmlschema:
        xmlschema.write(schemaToXML(sch, dep, pk))

def translateSQLtoXMLfile(conf):
    pk=IncludePKs.NO
    if conf.p:
        pk=IncludePKs.AS_PK
    elif conf.f:
        pk=IncludePKs.AS_EGD
    schema = sqlToSchema(conf.infile)
    if conf.outfile:
        writeXMLForSchema(schema, conf.outfile, pk=pk)
    else:
        print(schemaToXML(schema,'', pk))

def TPCH():
    sch = sqlToSchema("./tpch/tpch.sql")
    writeXMLForSchema("./tpch/full_tpch_schema.xm")

def main():
    parser = ap.ArgumentParser(description='Create PDQs XML schemas.')
    subparsers = parser.add_subparsers()

    # translate SQL to XML
    sqltoxml_parser = subparsers.add_parser('translate_sql')
    sqltoxml_parser.add_argument("-i", "--infile", type=str, help='input SQL file (no newlines because of limitation of sqlparse library', required=True)
    sqltoxml_parser.add_argument("-o", "--outfile", type=str, help='output XML file. If no file is provided write to stdout', required=False)
    sqltoxml_parser.add_argument("-p", action='store_true', help='output primary key constraints in relation elements')
    sqltoxml_parser.add_argument("-f", action='store_true', help='output PK constraints as EGDs')
    sqltoxml_parser.set_defaults(func=translateSQLtoXMLfile)

    # call function for subcommand
    conf = parser.parse_args()
    print(conf)
    conf.func(conf)

if __name__ == '__main__':
    main()
