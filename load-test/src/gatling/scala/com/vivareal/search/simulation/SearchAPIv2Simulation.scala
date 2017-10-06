package com.vivareal.search.simulation

import com.typesafe.config.ConfigFactory
import com.vivareal.search.config.SearchAPIv2Feeder
import com.vivareal.search.repository.SearchAPIv2Repository
import com.vivareal.search.repository.SearchAPIv2Repository.ScenarioName._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class SearchAPIv2Simulation extends Simulation {

  val config = ConfigFactory.load()

  val gatling = config.getConfig("gatling")

  val httpConf = http.baseURL(s"http://${config.getString("api.http.base")}")

  val api = config.getConfig("api")

  val index = config.getString("api.http.listings")

  val scenarios = gatling.getString("scenarios").split(",")
    .toList
      .map {
        case FILTERS => scenario(FILTERS)
          .repeat(gatling.getInt("repeat")) {
            feed(SearchAPIv2Feeder(SearchAPIv2Repository.getFacets).random)
              .exec(http("Filter ${name}").get(index + "?filter=${name}:\"${value}\""))
          }

        case FACETS => scenario(FACETS)
          .repeat(gatling.getInt("repeat")) {
            feed(SearchAPIv2Feeder(SearchAPIv2Repository.getFacets).random)
              .exec(http("Facet ${name}").get(index + "?filter=${name}:\"${value}\"&facets=${name}"))
          }

        case IDS => scenario(IDS)
          .repeat(gatling.getInt("repeat")) {
            feed(SearchAPIv2Feeder(SearchAPIv2Repository.getIds))
              .exec(http("By ID").get(index + "/${value}"))
          }
      }
    .map(scn => scn.inject(rampUsers(gatling.getInt(s"${scn.name}.users")) over (gatling.getInt("rampUp") seconds)))

  setUp(scenarios)
    .protocols(httpConf)
    .maxDuration(gatling.getInt("maxDuration") seconds)
}
