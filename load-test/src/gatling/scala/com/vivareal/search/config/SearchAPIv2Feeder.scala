package com.vivareal.search.config

import com.typesafe.config.ConfigFactory

import scala.util.Random

object SearchAPIv2Feeder {
  val config = ConfigFactory.load()

  def filters(values: List[(String, String)]): Array[Map[String, String]] =
    Random.shuffle(values).map(tuple => Map("name" -> tuple._1, "value" -> tuple._2)).toArray

  def ids(values: Iterator[String]): Array[Map[String, String]] =
    values.map(value => Map("value" -> value)).toArray

  def idsIN(values: Iterator[String]): Array[Map[String, String]] = {
    val range = config.getInt("gatling.idsIn.range")

    values
      .sliding(range, range)
      .map(ids => Map("value" -> ids.mkString(",")))
      .toArray
  }
}
