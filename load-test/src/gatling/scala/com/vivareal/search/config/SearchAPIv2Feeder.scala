package com.vivareal.search.config

import scala.util.Random

object SearchAPIv2Feeder {
  def apply(values: List[(String, String)]): Array[Map[String, String]] =
    Random.shuffle(values).map(tuple => Map("name" -> tuple._1, "value" -> tuple._2)).toArray

  def apply(values: Iterator[String]): Array[Map[String, String]] =
    values.map(value => Map("value" -> value)).toArray
}
