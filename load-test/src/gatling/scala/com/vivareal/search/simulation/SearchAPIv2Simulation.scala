package com.vivareal.search.simulation

import com.typesafe.config.ConfigFactory
import com.vivareal.search.config.SearchAPIv2Feeder
import com.vivareal.search.repository.SearchAPIv2Repository
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class SearchAPIv2Simulation extends Simulation {

  val config = ConfigFactory.load()

  val gatling = config.getConfig("gatling")

  val httpConf = http.baseURL(s"http://${config.getString("api.http.base")}")

  val api = config.getConfig("api")

  val index = config.getString("api.http.listings")

  val filters = scenario("Filters")
    .repeat(gatling.getInt("repeat")) {
      feed(SearchAPIv2Feeder(SearchAPIv2Repository.getFacets).random)
        .exec(http("Filter ${name}").get(index + "?filter=${name}:\"${value}\""))
    }

  val facets = scenario("Facets")
    .repeat(gatling.getInt("repeat")) {
      feed(SearchAPIv2Feeder(SearchAPIv2Repository.getFacets).random)
        .exec(http("Facet ${name}").get(index + "?filter=${name}:\"${value}\"&facets=${name}"))
    }

  val byID = scenario("Find by ID")
    .repeat(gatling.getInt("repeat")) {
      feed(SearchAPIv2Feeder(SearchAPIv2Repository.getIds))
        .exec(http("By ID").get(index + "/${value}"))
    }

  setUp(filters.inject(rampUsers(gatling.getInt("users")) over (gatling.getInt("rampUp") seconds)),
    facets.inject(rampUsers(gatling.getInt("facets.users")) over (gatling.getInt("rampUp") seconds)),
    byID.inject(rampUsers(gatling.getInt("byid.users")) over (gatling.getInt("rampUp") seconds)))
    .protocols(httpConf)
    .maxDuration(gatling.getInt("maxDuration") seconds)
}
