create table Activity
(
   activity_comment varchar(20),
   activity_id varchar(20),
   assay_chembl_id varchar(20),
   assay_description varchar(20),
   assay_type varchar(20),
   bao_endpoint varchar(20),
   bao_format varchar(20),
   canonical_smiles varchar(20),
   data_validity_comment varchar(20),
   document_chembl_id varchar(20),
	    document_journal varchar(20),
		document_year varchar(20),
		molecule_chembl_id varchar(20),
		pchembl_value varchar(20),
		potential_duplicate varchar(20),
		published_relation varchar(20),
		published_type varchar(20),
		published_units varchar(20),
		published_value varchar(20),
		qudt_units varchar(20),
		record_id varchar(20),
		standard_flag varchar(20),
		standard_relation varchar(20),
		standard_type varchar(20),
		standard_units varchar(20),
		standard_value varchar(20),
		target_chembl_id varchar(20),
		target_organism varchar(20),
		target_pref_name varchar(20),
		uo_units varchar(20)
);
insert into public.Activity values('1','2','3','4','5','6','7','8','9','10', '11','12','13','14','15','16','17','18','19','20', '21','22','23','24','25','26','27','28','29','30');
create table public.supplier
(
 s_suppkey varchar(20),
 s_name varchar(20),
 s_address varchar(20),
 s_nationkey varchar(20),
 s_phone varchar(20),
 s_acctbal varchar(20),
 s_comment varchar(20)
);
insert into public.supplier values('1','2','3','4','5','6','7');
create table public.partsupp
(
 ps_partkey varchar(20),
 ps_suppkey varchar(20),
 ps_availqty varchar(20),
 ps_supplycost varchar(20),
 ps_comment varchar(20)
);
insert into public.partsupp values('1','1','2','3','4');
create table region_nation
(
 nation_key varchar(20),
 nation_name varchar(20),
 region_key varchar(20),
 region_name varchar(20) 
);
insert into public.region_nation values('4','5','6','7');
create table public.nation (n_nationkey integer, n_name varchar(1024), n_regionkey integer, n_comment varchar(1024))
insert into public.nation (n_nationkey, n_name, n_regionkey, n_comment) values(1, 'USA', 0, 'comment')
insert into public.nation (n_nationkey, n_name, n_regionkey, n_comment) values(2, 'USSR', 1, 'comment')
create table public.country (c_nationkey integer, c_area integer)
insert into public.country (c_nationkey, c_area) values(1, 3794000), (2, 6593000)
       create table activityFree(
            activity_comment varchar(20),
            activity_id varchar(20),
            assay_chembl_id varchar(20),
            assay_description varchar(20),
            assay_type varchar(20),
            bao_endpoint varchar(20),
            bao_format varchar(20),
            canonical_smiles varchar(20),
            data_validity_comment varchar(20),
            document_chembl_id varchar(20),
            document_journal varchar(20),
            document_year varchar(20),
            molecule_chembl_id varchar(20),
            pchembl_value float,
            potential_duplicate varchar(20),
            published_relation varchar(20),
            published_type varchar(20),
            published_units varchar(20),
            published_value varchar(20),
            qudt_units varchar(20),
            record_id varchar(20),
            standard_flag boolean,
            standard_relation varchar(20),
            standard_type varchar(20),
            standard_units varchar(20),
            standard_value float,
            target_chembl_id varchar(20),
            target_organism varchar(20),
            target_pref_name varchar(20),
            uo_units varchar(20));
			insert into activityFree values('A','B','C','D','E','F','G','H','I','J','K','L','M',0.0,
											'N','O','P','Q','R','S','T',true,'U','V','W',0.0,'X','Y','Z','ZZ');
create table r (x int);
create table s (x int, y int);
create table t (y int, z int, w int);
insert into r values(0),(4);
insert into s values(0, 1), (4, 5);
insert into t values(1, 2, 3), (5, 6, 7);
