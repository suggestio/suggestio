package util.qsb

import play.api.mvc.QueryStringBindable
import models._

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.14 17:48
 * Description: Здесь складываются небольшие QueryStringBindable для сложных get-реквестов.
 */

object QsbUtil {

  implicit def eitherOpt2option[T](e: Either[_, Option[T]]): Option[T] = {
    e.fold({_ => None}, identity)
  }

}


object QSBs extends JavaTokenParsers {

  private def companyNameSuf = ".meta.name"

  /** qsb для MCompany. */
  implicit def mcompanyQSB(implicit strBinder: QueryStringBindable[String]) = {
    new QueryStringBindable[MCompany] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MCompany]] = {
        for {
          maybeCompanyName <- strBinder.bind(key + companyNameSuf, params)
        } yield {
          maybeCompanyName.right.map { companyName =>
            MCompany(meta = MCompanyMeta(name = companyName))
          }
        }
      }

      override def unbind(key: String, value: MCompany): String = {
        strBinder.unbind(key + companyNameSuf, value.meta.name)
      }
    }
  }


  /** qsb для NodeGeoLevel, записанной в виде int или string (esfn). */
  implicit def nodeGeoLevelQSB(implicit strB: QueryStringBindable[String], intB: QueryStringBindable[Int]) = {
    new QueryStringBindable[NodeGeoLevel] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, NodeGeoLevel]] = {
        val nglOpt: Option[NodeGeoLevel] = intB.bind(key, params)
          .filter(_.isRight)
          .flatMap { eith => NodeGeoLevels.maybeWithId(eith.right.get) }
          .orElse {
            strB.bind(key, params)
              .filter(_.isRight)
              .flatMap { eith => NodeGeoLevels.maybeWithName(eith.right.get) }
          }
        val result = nglOpt match {
          case Some(ngl) =>
            Right(ngl)
          case None =>
            Left("Unknown geo level id")
        }
        Some(result)
      }

      override def unbind(key: String, value: NodeGeoLevel): String = {
        intB.unbind(key, value.id)
      }
    }
  }


  private val picSizeNumRe = "\\d{2,5}".r
  private val picSizeDelimRe = "[xX]"

  private def sizeP: Parser[MImgInfoMeta] = {
    val sizeP: Parser[Int] = picSizeNumRe ^^ { _.toInt }
    sizeP ~ (picSizeDelimRe ~> sizeP) ^^ {
      case w ~ h  =>  MImgInfoMeta(width = w, height = h)
    }
  }

  /** qsb для бинда значения длины*ширины из qs. */
  implicit def mImgInfoMetaQsb(implicit strB: QueryStringBindable[String]) = {
    new QueryStringBindable[MImgInfoMeta] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MImgInfoMeta]] = {
        strB.bind(key, params).map { maybeWxh =>
          maybeWxh.right.flatMap { wxh =>
            val pr = parse(sizeP, wxh)
            if (pr.successful)
              Right(pr.get)
            else
              Left("Unsupported screen size format.")
          }
        }
      }

      override def unbind(key: String, value: MImgInfoMeta): String = {
        s"${value.width}x${value.height}"
      }
    }
  }

}
