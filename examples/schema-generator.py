#!/bin/env python

from functools import reduce
import operator
from dataclasses import dataclass

datatype_to_java = {
     'NUMBER' : 'java.lang.Integer',
     'VARCHAR' : 'java.lang.String'
     }

access_method_counter = 1

@dataclass
class Table:
    name: str
    attrs: list(tuple(str,str))

@dataclass
class FD:
    lhs: list(str)
    rhs: list(str)

@dataclass
class atom:
    name: str
    args: list(str)

    def toXML(indent=3):
        varStrs = listConcat([ '\t' + varToXML(arg) for arg in args])
        return listConcat([ indent * 't' + s for s in  f"""<atom name="S">
{varStrs}
</atom>""".split("\n")])

@dataclass
class TGD:
    lhs: list(atom)
    rhs: list(atom)

    def toXML():
        return f"""<dependency type="TGD">
	  <body>
{listConcat([ a.toXML() for a in lhs])}
      </body>
	  <head>
{listConcat([ a.toXML() for a in rhs])}
      </head>
    </dependency>
"""

@dataclass
class EDG:
    lhs: list(atom)
    rhs: list(tuple(str,str))

    @staticmethod
    def equalityToXML(eq):
        return f"""
        <atom name="EQUALITY">
		  <variable name="{eq[0]}" />
		  <variable name="{eq[1]}" />
	    </atom>
"""

    def toXML():
        return f"""<dependency>
	  <body>
{listConcat([ a.toXML() for a in lhs])}
	  </body>
	  <head>
{listConcat([ EDG.equalityToXML(e) for e in rhs])}
	  </head>
    </dependency>
"""

def dtToJava(dt):
    global datatype_to_java
    return datatype_to_java[dt]

def attrToXML(name, dt):
    return f'<attribute name="{name}" type="{dtToJava(dt)}"/>'

def tableToXML(table):
    global access_method_counter
    access_method_counter += 1
    accm = f'm{access_method_counter}'
    attrStr = listConcat([ "\t\t" + attrToXML(x) for x in table.attrs])
    return f"""<relation name="{table.name}">
{attrStr}
\t\t<access-method name="{accm}"/>
\t</relation>
"""

def tableViewToXML(table):
    attrStr = listConcat([ "\t\t" + attrToXML(x) for x in table.attrs])
    return f"""<view name="{'v' + table.name}">
{attrStr}
\t</view>
"""

def varToXML(name):
    return f'<variable name="{name}" />'

def createVarList(attrs, tabs):
    return reduce(operator.add, [ (tabs * '\t') + varToXML(i) for i in range(1,len(attrs)) ], "")

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
    return reduce(lambda x,y: x + y, strl, '')

def depToXML(d):
    return d.toXML()

def schemaToXML(tables, deps):
    strtable = [ tableToXML(t) for t in tables ]
    strviews = [ tableViewToXML(t) for t in tables ]
    strdeps = [ createTableViewDep(t) for t in tables ]
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

def main():
    deps = []
    tables = []
    views = []

if __name__ == '__main__':
    main()
