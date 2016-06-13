-- MySQL dump 10.13  Distrib 5.7.12, for osx10.11 (x86_64)
--
-- Host: localhost    Database: addresses
-- ------------------------------------------------------
-- Server version	5.7.12

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `address`
--

DROP TABLE IF EXISTS `address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `address` (
  `id` char(10) NOT NULL DEFAULT '',
  `seq` char(3) NOT NULL DEFAULT '',
  `name` char(30) DEFAULT NULL,
  `prefix` char(2) DEFAULT NULL,
  `type` char(4) DEFAULT NULL,
  `startlat` float(12,8) NOT NULL DEFAULT '0.00000000',
  `startlong` float(12,8) NOT NULL DEFAULT '0.00000000',
  `endlat` float(12,8) NOT NULL DEFAULT '0.00000000',
  `endlong` float(12,8) NOT NULL DEFAULT '0.00000000',
  `leftzip` int(5) DEFAULT NULL,
  `rightzip` int(5) DEFAULT NULL,
  `leftaddr1` char(11) DEFAULT NULL,
  `leftaddr2` char(11) DEFAULT NULL,
  `rightaddr1` char(11) DEFAULT NULL,
  `rightaddr2` char(11) DEFAULT NULL,
  `name_dtmf` char(30) DEFAULT NULL,
  `type_dtmf` char(4) DEFAULT NULL,
  `prefix_dtmf` char(2) DEFAULT NULL,
  `address_id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`address_id`),
  UNIQUE KEY `unique_index` (`id`,`seq`),
  KEY `name` (`name`),
  KEY `leftzip` (`leftzip`),
  KEY `rightzip` (`rightzip`)
) ENGINE=MyISAM AUTO_INCREMENT=3221317 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock`
--

DROP TABLE IF EXISTS `stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stock` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `company` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `date` date NOT NULL,
  `open` float NOT NULL,
  `high` float NOT NULL,
  `low` float NOT NULL,
  `close` float NOT NULL,
  `volume` int(11) NOT NULL,
  `adj_close` float NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4195 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-06-13 21:32:07
