package com.twitter.finagle.http

import org.specs.Specification


object ParamsMapSpec extends Specification {
  "ParamsMap" should {
    "no params" in {
      val request = Request("/search.json")
      request.params.get("q") must_== None
    }

    "no params, just question mark" in {
      val request = Request("/search.json")
      request.params.get("q") must_== None
    }

    "params" in {
      val request = Request("/search.json?q=twitter")
      request.params.get("q") must_== Some("twitter")
    }

    "getInt" in {
      Request("?x=1").params.getInt("x")           must_== Some(1)
      Request("?x=0").params.getInt("x")           must_== Some(0)
      Request("?x=-1").params.getInt("x")          must_== Some(-1)
      Request("?x=2147483647").params.getInt("x")  must_== Some(2147483647)
      Request("?x=-2147483648").params.getInt("x") must_== Some(-2147483648)
      Request("?x=2147483648").params.getInt("x")  must_== Some(0)
      Request("?x=-2147483649").params.getInt("x") must_== Some(0)
      Request("?x=garbage").params.getInt("x")     must_== Some(0)
      Request("?x=").params.getInt("x")            must_== Some(0)
      Request("?y=2").params.getInt("x")           must_== None
    }

    "getLong" in {
      Request("?x=1").params.getLong("x")                    must_== Some(1L)
      Request("?x=0").params.getLong("x")                    must_== Some(0L)
      Request("?x=-1").params.getLong("x")                   must_== Some(-1L)
      Request("?x=9223372036854775807").params.getLong("x")  must_== Some(9223372036854775807L)
      Request("?x=-9223372036854775808").params.getLong("x") must_== Some(-9223372036854775808L)
      Request("?x=9223372036854775808").params.getLong("x")  must_== Some(0L)
      Request("?x=-9223372036854775809").params.getLong("x") must_== Some(0L)
      Request("?x=garbage").params.getLong("x")              must_== Some(0L)
      Request("?x=").params.getLong("x")                     must_== Some(0L)
      Request("?y=2").params.getLong("x")                    must_== None
    }

    "getBoolean" in {
      Request("?x=true").params.getBoolean("x")    must_== Some(true)
      Request("?x=TRUE").params.getBoolean("x")    must_== Some(true)
      Request("?x=True").params.getBoolean("x")    must_== Some(true)
      Request("?x=t").params.getBoolean("x")       must_== Some(true)
      Request("?x=1").params.getBoolean("x")       must_== Some(true)
      Request("?x=false").params.getBoolean("x")   must_== Some(false)
      Request("?x=").params.getBoolean("x")        must_== Some(false)
      Request("?x=garbage").params.getBoolean("x") must_== Some(false)
      Request("?y=2").params.getBoolean("x")       must_== None
    }

    "params encoded" in {
      val request = Request("/search.json?%71=%74%77%69%74%74%65%72")
      request.params.get("q") must_== Some("twitter")
    }

    "params and equals encoded" in {
      val request = Request("/search.json?%71%3D%74%77%69%74%74%65%72")
      request.params.get("q") must_== None
    }

    "multiple params" in {
      val request = Request("/search.json?q=twitter&lang=en")
      request.params.get("q")    must_== Some("twitter")
      request.params.get("lang") must_== Some("en")
    }

    "key, no value" in {
      val request = Request("/search.json?q=")
      request.params.get("q") must_== Some("")
    }

    "value, no key is ignored" in {
      val request = Request("/search.json?=value")
      request.params.get("") must_== None
    }

    "favor first value" in {
      val request = Request("/search.json?q=twitter&q=twitter2")
      request.params.get("q") must_== Some("twitter")
    }

    "getAll" in {
      val request = Request("/search.json?q=twitter&q=twitter2")
      request.params.getAll("q").toList must_== "twitter" :: "twitter2" :: Nil
    }

    "iterator" in {
      val request = Request("/search.json?q=twitter&q=twitter2&lang=en")
      request.params.iterator.toList.sorted must_==
        ("lang", "en") :: ("q", "twitter") :: ("q", "twitter2") :: Nil
    }

    "plus" in {
      val request = Request("/search.json?q=twitter")
      val params = request.params + ("lang" -> "en")
      params.get("q")    must_== Some("twitter")
      params.get("lang") must_== Some("en")
    }

    "minus" in {
      val request = Request("/search.json?q=twitter")
      val params = request.params - "q"
      params.get("q") must_== None
    }

    "empty" in {
      val request = Request("/search.json?q=twitter")
      val params = request.params.empty
      params.get("q") must_== None
    }

    "toString" in {
      Request("/search.json?q=twitter").params.toString must_== "?q=twitter"
      Request("/search.json").params.toString           must_== ""
    }


    "get, POST params" in {
      val request = Request(Method.Post, "/search.json")
      request.contentType = "application/x-www-form-urlencoded"
      request.contentString = "q=twitter"
      request.params.get("q") must_== Some("twitter")
    }

    "getAll, POST params" in {
      val request = Request(Method.Post, "/search.json?q=twitter2")
      request.contentType = "application/x-www-form-urlencoded"
      request.contentString = "q=twitter"
      request.params.get("q") must_== Some("twitter") // favor POST param
      request.params.getAll("q").toList.sorted must_== "twitter" :: "twitter2" :: Nil
    }

    "ignore body when not a POST" in {
      val request = Request("/search.json?lang=en")
      request.contentType = "application/x-www-form-urlencoded"
      request.contentString = "q=twitter"
      request.params.get("q")    must_== None
      request.params.get("lang") must_== Some("en")
    }

    "weird encoded characters" in {
      for (i <- 0x7f until 0xff) {
        val getRequest = Request("/search.json?q=%%02x".format(i))
        getRequest.params.get("q") must beSomething

        val postRequest = Request(Method.Post, "/search.json")
        postRequest.contentType = "application/x-www-form-urlencoded"
        postRequest.contentString = "q=%%02x".format(i)
        postRequest.params.get("q") must beSomething
      }
    }

    "illegal hex characters" in {
      val request = Request("/search.json?q=%u3")
      request.params.isValid must beFalse
    }

    "incomplete trailing escape" in {
      val request = Request("/search.json?q=%3")
      request.params.isValid must beFalse
    }

    "quotes are ok" in {
      // Java's URL doesn't allow this, but we do.
      val request = Request("/search.json?q=\"twitter\"")
      request.params.get("q") must_== Some("\"twitter\"")
    }
  }
}
