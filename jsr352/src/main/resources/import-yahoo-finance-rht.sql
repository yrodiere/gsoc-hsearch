--
-- Import Yahoo Finance's historical prices in CSV
-- using company Red Hat, Inc. (RHT)
--
-- http://finance.yahoo.com/q/hp?s=RHT&a=07&b=11&c=1999&d=03&e=13&f=2016&g=d
--
LOAD DATA LOCAL INFILE '/Users/mincong/Downloads/yahoo-finance-rht.csv'
  INTO TABLE stock
  FIELDS TERMINATED BY ',' ENCLOSED BY ''
  IGNORE 1 LINES
  (`date`, `open`, `high`, `low`, `close`, `volume`, `adj_close`)
  SET company = 'RHT';
