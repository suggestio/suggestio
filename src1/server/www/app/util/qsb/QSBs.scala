package util.qsb

import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.dev.PicSzParsers
import io.suggest.geo.{MNodeGeoLevel, MNodeGeoLevels}
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.util.logs.MacroLogsImplLazy
import play.api.mvc.QueryStringBindable

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
                               intB: QueryStringBindable[Int]): QueryStringBindable[MNodeGeoLevel] = {
    new QueryStringBindableImpl[MNodeGeoLevel] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MNodeGeoLevel]] = {
        val result = (for {
          bindedE <- intB.bind(key, params)
          binded  <- bindedE.toOption
          ngl <- MNodeGeoLevels.withIdOption( binded )
        } yield ngl)
          .orElse {
            for {
              bindedE <- strB.bind(key, params)
              binded  <- bindedE.toOption
              ngl     <- MNodeGeoLevels.withNameOption( binded )
            } yield ngl
          }
          .toRight( "Unknown geo level id" )

        Some(result)
      }

      override def unbind(key: String, value: MNodeGeoLevel): String = {
        intB.unbind(key, value.id)
      }
    }
  }

  def parseWxH(wxh: String): ParseResult[MSize2di] = {
    parse(whP, wxh)
  }

  def unParseWxH(value: ISize2di): String = {
    ISize2di.toString( value )
  }

  /** qsb для бинда значения длины*ширины из qs. */
  implicit def mImgInfoMetaQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MSize2di] = {
    new QueryStringBindableImpl[MSize2di] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MSize2di]] = {
        strB.bind(key, params).map { maybeWxh =>
          maybeWxh.flatMap { wxh =>
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


  type NglsStateMap_t = Map[MNodeGeoLevel, Boolean]

  implicit def nglsMapQsb: QueryStringBindable[NglsStateMap_t] = {
    new QueryStringBindableImpl[NglsStateMap_t] with MacroLogsImplLazy {
      import LOGGER._

      def vP: Parser[Boolean] = opt("_" ^^^ false) ^^ { _ getOrElse true }
      def kP: Parser[MNodeGeoLevel] = "[a-z]{2}".r ^^ MNodeGeoLevels.withName
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
