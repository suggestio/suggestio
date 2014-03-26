package util.qsb

import play.api.mvc.QueryStringBindable
import models._
import io.suggest.ym.model.AdsSearchT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.14 17:48
 * Description: Здесь складываются небольшие QueryStringBindable для сложных get-реквестов.
 */

object AdSearch {

  private implicit def eitherOpt2option[T](e: Either[_, Option[T]]): Option[T] = {
    e match {
      case Left(_)  => None
      case Right(b) => b
    }
  }

  implicit def queryStringBinder(implicit strOptBinder: QueryStringBindable[Option[String]]) = new QueryStringBindable[AdSearch] {
    def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AdSearch]] = {
      for {
        maybeShopIdOpt <- strOptBinder.bind(key + ".shopId", params)
        maybeCatIdOpt  <- strOptBinder.bind(key + ".catId", params)
        maybeLevelOpt  <- strOptBinder.bind(key + ".level", params)
        maybeQOpt      <- strOptBinder.bind(key + ".q", params)
      } yield {
        Right(
          AdSearch(
            shopIdOpt = maybeShopIdOpt,
            catIdOpt  = maybeCatIdOpt,
            levelOpt  = maybeLevelOpt.flatMap(AdShowLevels.maybeWithName),
            qOpt      = maybeQOpt
          )
        )
      }
    }

    def unbind(key: String, value: AdSearch): String = {
      strOptBinder.unbind(key + ".shopId", value.shopIdOpt) + "&" +
      strOptBinder.unbind(key + ".catId", value.catIdOpt) + "&" +
      strOptBinder.unbind(key + ".level", value.levelOpt.map(_.toString)) + "&" +
      strOptBinder.unbind(key + ".q", value.qOpt)
    }
  }

}

case class AdSearch(
  shopIdOpt: Option[ShopId_t] = None,
  catIdOpt: Option[String] = None,
  levelOpt: Option[AdShowLevel] = None,
  qOpt: Option[String] = None
) extends AdsSearchT


// Pager - пример, взятый из scaladoc
object Pager {

  implicit def queryStringBinder(implicit intBinder: QueryStringBindable[Int]) = new QueryStringBindable[Pager] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Pager]] = {
      for {
        index <- intBinder.bind(key + ".index", params)
        size <- intBinder.bind(key + ".size", params)
      } yield {
        (index, size) match {
          case (Right(index), Right(size)) => Right(Pager(index, size))
          case _ => Left("Unable to bind a Pager")
        }
      }
    }

    override def unbind(key: String, pager: Pager): String = {
      intBinder.unbind(key + ".index", pager.index) + "&" + intBinder.unbind(key + ".size", pager.size)
    }
  }

}

case class Pager(index: Int, size: Int)
