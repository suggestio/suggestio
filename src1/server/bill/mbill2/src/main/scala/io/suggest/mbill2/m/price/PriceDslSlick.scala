package io.suggest.mbill2.m.price

import io.suggest.bill.price.dsl.PriceDsl
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.slick.profile.IProfile
import play.api.libs.json.{JsValue, Json}
import scalaz.Tree

trait PriceDslSlick extends IProfile {

  import profile.api._

  def PRICE_DSL_FN = "price_dsl"

  trait PriceDslColumn { that: Table[_] =>

    def priceDslJsonOpt = column[Option[JsValue]]( PRICE_DSL_FN )

    /** Mapped column projection of PriceDsl JSON tree. */
    def priceDslOpt = priceDslJsonOpt <> (
      {jsValueOpt =>
        jsValueOpt.map( _.as[Tree[PriceDsl]] )
      },
      {priceDslOpt: Option[Tree[PriceDsl]] =>
        priceDslOpt.map { priceDsl =>
          Some( Json.toJson(priceDsl) )
        }
      }
    )

  }

}
