CREATE TABLE IF NOT EXISTS <schema>.`gsusers` (
  `pkgsusers` INT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `name` VARCHAR(45) NOT NULL DEFAULT 'foobar' ,
  `buyItem` INT UNSIGNED NOT NULL DEFAULT 0 ,
  `buyQty` INT UNSIGNED NOT NULL DEFAULT 1 ,
  `sellItems` VARCHAR(45) NOT NULL DEFAULT '' ,
  `lastBought` TIMESTAMP NULL ,
  `lastSold` TIMESTAMP NULL ,
  PRIMARY KEY (`pkgsusers`) ,
  UNIQUE INDEX `name_UNIQUE` (`name` ASC)
  ) ENGINE = InnoDB DEFAULT CHARACTER SET = latin1 ;
ALTER TABLE <schema>.`gslog` CHANGE COLUMN `user` `user` INT UNSIGNED NULL  , 
  ADD CONSTRAINT `fk_user`
  FOREIGN KEY (`user` )
  REFERENCES `minecraft`.`gsusers` (`pkgsusers` )
  ON DELETE SET NULL
  ON UPDATE CASCADE
, ADD INDEX `fk_user` (`user` ASC) ;
