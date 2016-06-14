LOAD DATA LOCAL INFILE '/Users/mincong/Documents/aws-addresses-a-0-5m.csv'
  INTO TABLE address
  FIELDS TERMINATED BY ',' ENCLOSED BY '"';
