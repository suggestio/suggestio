package util.qsb

import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.util.logs.MacroLogsImplLazy
import play.api.mvc.QueryStringBindable
import models._
import util.img.PicSzParsers

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.14 17:48
 * Description: Здесь складываются небольшие QueryStringBindable для сложных get-реквестов.
 */

object QsbUtil {

  // TODO Спилить эту утиль. От неё больше неочевидности, нежели пользы.

  def eitherOpt2option[T](e: Either[_, Option[T]]): Option[T] = {
    e.fold({_ => None}, identity)
  }

}


object QSBs extends JavaTokenParsers with PicSzParsers {

  /** qsb для NodeGeoLevel, записанной в виде int или string (esfn). */
  implicit def nodeGeoLevelQSB(implicit strB: QueryStringBindable[String],
                               intB: QueryStringBindable[Int]): QueryStringBindable[NodeGeoLevel] = {
    new QueryStringBindableImpl[NodeGeoLevel] {
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


  def sizeP: Parser[MSize2di] = {
    resolutionRawP ^^ {
      case w ~ h  =>  MSize2di(width = w, height = h)
    }
  }
  
  def parseWxH(wxh: String): ParseResult[MSize2di] = {
    parse(sizeP, wxh)
  }

  def unParseWxH(value: ISize2di): String = {
    ISize2di.toString( value )
  }

  /** qsb для бинда значения длины*ширины из qs. */
  implicit def mImgInfoMetaQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MSize2di] = {
    new QueryStringBindableImpl[MSize2di] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MSize2di]] = {
        strB.bind(key, params).map { maybeWxh =>
          maybeWxh.right.flatMap { wxh =>
            val pr = parseWxH(wxh)
            if (pr.successful)
              Right(pr.get)
            else
              Left("Unsupported screen size format.")
          }
        }
      }

      override def unbind(key: String, value: MSize2di): String = {
        unParseWxH(value)
      }
    }
  }


  type NglsStateMap_t = Map[NodeGeoLevel, Boolean]

  implicit def nglsMapQsb: QueryStringBindable[NglsStateMap_t] = {
    new QueryStringBindableImpl[NglsStateMap_t] with MacroLogsImplLazy {
      import LOGGER._

      def vP: Parser[Boolean] = opt("_" ^^^ false) ^^ { _ getOrElse true }
      def kP: Parser[NodeGeoLevel] = "[a-z]{2}".r ^^ NodeGeoLevels.withName
      def kvP = (kP ~ vP) ^^ { case k ~ v => (k, v) }
      def mapP = rep(kvP) ^^ { _.toMap }

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, NglsStateMap_t]] = {
        params.get(key)
          .flatMap(_.find(!_.isEmpty))
          .flatMap { raw =>
            parseAll(mapP, raw) match {
              case Success(s, _) =>
                Some(Right(s))
              case noSuccess =>
                warn(s"Suppressed failure during parsing of ngls map passed: $raw ;; $noSuccess")
                None
            }
          }
      }

      override def unbind(key: String, value: NglsStateMap_t): String = {
        val sb = new StringBuilder(key)
          .append('=')
        for ((ngl, flag) <- value) {
          sb.append(ngl.esfn)
          if (!flag)
            sb.append('_')
        }
        sb.toString()
      }
    }
  }

}
