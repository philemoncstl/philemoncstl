ALTER TABLE ct ADD free_time_unit1 INT NULL;

CREATE TABLE `iuc_event_log` (
  `eid` int(11) NOT NULL AUTO_INCREMENT,
  `ct_id` int(11) NOT NULL,
  `event_type` varchar(30) COLLATE utf8_estonian_ci NOT NULL,
  `event_dttm` datetime NOT NULL,
  `remark` varchar(100) COLLATE utf8_estonian_ci DEFAULT NULL,
  `cre_dttm` datetime NOT NULL,
  PRIMARY KEY (`eid`)
) ENGINE=InnoDB AUTO_INCREMENT=15948 DEFAULT CHARSET=utf8 COLLATE=utf8_estonian_ci AVG_ROW_LENGTH=103;


ALTER TABLE iuc_event_log ADD CONSTRAINT iuc_event_log_FK FOREIGN KEY (ct_id) REFERENCES `sg-evcshhltest`.ct(ct_id);


