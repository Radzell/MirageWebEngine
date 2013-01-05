use test;
drop database if exists miragedb;
create database if not exists miragedb;
use miragedb;

create table targetimage (
id int not null auto_increment,
_name nvarchar(100), 
_author nvarchar(100), 
_description text character set utf8,
_rating float,
_rateCount int,
_image blob,
_bigImage longblob,
_width int,
_height int,
_keypoint longblob,
_descriptor longblob,
primary key(id)
);


