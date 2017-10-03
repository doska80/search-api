package com.vivareal.search.simulation

import com.typesafe.config.ConfigFactory
import com.vivareal.search.config.SearchAPIv2Feeder
import com.vivareal.search.repository.SearchAPIv2Repository
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class SearchAPIv2Simulation extends Simulation {

  val config = ConfigFactory.load()

  val gatling = config.getConfig("gatling")

  val httpConf = http.baseURL(s"http://${config.getString("api.http.base")}")

  val api = config.getConfig("api")

  val index = config.getString("api.http.listings")

  val scenariosMap = Map(
    "filters" -> scenario("Filters")
    .repeat(gatling.getInt("repeat")) {
      feed(SearchAPIv2Feeder(SearchAPIv2Repository.getFacets).random)
        .exec(http("Filter ${name}").get(index + "?filter=${name}:\"${value}\""))
    },

    "facets" -> scenario("Facets")
      .repeat(gatling.getInt("repeat")) {
        feed(SearchAPIv2Feeder(SearchAPIv2Repository.getFacets).random)
          .exec(http("Facet ${name}").get(index + "?filter=${name}:\"${value}\"&facets=${name}"))
      },

    "ids" -> scenario("Find by ID")
      .repeat(gatling.getInt("repeat")) {
        feed(SearchAPIv2Feeder(SearchAPIv2Repository.getIds))
          .exec(http("By ID").get(index + "/${value}"))
      }
  )

  val scenarios = gatling.getStringList("scenarios")
    .asScala
    .map(scn => scenariosMap(scn).inject(rampUsers(gatling.getInt(s"$scn.users")) over (gatling.getInt("rampUp") seconds)))
    .toList

  setUp(scenarios)
    .protocols(httpConf)
    .maxDuration(gatling.getInt("maxDuration") seconds)
}
