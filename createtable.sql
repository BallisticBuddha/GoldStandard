CREATE TABLE IF NOT EXISTS `gslog` (
`pkGSlog` int(10) unsigned NOT NULL AUTO_INCREMENT,
`time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
`amount` int(10) unsigned NOT NULL DEFAULT '1',
`user` varchar(45) DEFAULT NULL,
PRIMARY KEY (`pkGSlog`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1