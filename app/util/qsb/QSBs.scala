package util.qsb

import io.suggest.ym.model.common.{BlockMeta, IBlockMeta, MImgSizeT}
import play.api.mvc.QueryStringBindable
import models._
import util.PlayLazyMacroLogsImpl
import util.img.PicSzParsers
import util.secure.SecretGetter
import play.api.Play.{current, isProd}
import scala.language.implicitConversions

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


object QSBs extends JavaTokenParsers with PicSzParsers with AdsCssQsbUtil with BlockMetaBindable {

  private def companyNameSuf = ".meta.name"

  /** qsb для MCompany. */
  implicit def mcompanyQSB(implicit strBinder: QueryStringBindable[String]): QueryStringBindable[MCompany] = {
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
  implicit def nodeGeoLevelQSB(implicit strB: QueryStringBindable[String],
                               intB: QueryStringBindable[Int]): QueryStringBindable[NodeGeoLevel] = {
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


  def sizeP: Parser[MImgInfoMeta] = {
    resolutionRawP ^^ {
      case w ~ h  =>  MImgInfoMeta(width = w, height = h)
    }
  }
  
  def parseWxH(wxh: String): ParseResult[MImgInfoMeta] = {
    parse(sizeP, wxh)
  }

  def unParseWxH(value: MImgSizeT): String = {
    s"${value.width}x${value.height}"
  }

  /** qsb для бинда значения длины*ширины из qs. */
  implicit def mImgInfoMetaQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MImgInfoMeta] = {
    new QueryStringBindable[MImgInfoMeta] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MImgInfoMeta]] = {
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

      override def unbind(key: String, value: MImgInfoMeta): String = {
        unParseWxH(value)
      }
    }
  }


  type NglsStateMap_t = Map[NodeGeoLevel, Boolean]

  implicit def nglsMapQsb: QueryStringBindable[NglsStateMap_t] = {
    new QueryStringBindable[NglsStateMap_t] with PlayLazyMacroLogsImpl {
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
        val sb = new StringBuilder(key).append('=')
        value.foreach { case (ngl, flag) =>
          sb.append(ngl.esfn)
          if (!flag)
            sb.append('_')
        }
        sb.toString()
      }
    }
  }

}


/** Трейт с qsb для Seq[AdCssArgs] и сопутствующей утилью. */
trait AdsCssQsbUtil {

  private val SIGN_SECRET: String = {
    val sg = new SecretGetter with PlayLazyMacroLogsImpl {
      override val confKey = "ads.css.url.sign.key"
      override def useRandomIfMissing = isProd
    }
    sg()
  }

  /** Подписываемый QSB для списка AdCssArgs. */
  implicit def adsCssQsb = {
    new QueryStringBindable[Seq[AdCssArgs]] {
      private def getSigner = new QsbSigner(SIGN_SECRET, "sig")

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Seq[AdCssArgs]]] = {
        getSigner.signedOrNone(key, params)
          .flatMap(_.get(key))
          .map { vs =>
            try {
              val parsed = vs.map { v =>
                AdCssArgs.fromString(v)
              }
              Right(parsed)
            } catch {
              case ex: Exception =>
                Left(ex.getMessage)
            }
          }
      }

      override def unbind(key: String, value: Seq[AdCssArgs]): String = {
        val sb = new StringBuilder(30 * value.size)
        value.foreach { aca =>
          sb.append(key).append('=')
            .append(aca.adId).append(AdCssArgs.SEP_RE).append(aca.szMult)
            .append('&')
        }
        // Убрать финальный & из ссылки
        if (value.nonEmpty)
          sb.setLength(sb.length - 1)
        // Вернуть подписанный результат
        val res = sb.toString()
        getSigner.mkSigned(key, res)
      }
    }
  }

}


/** Трейт поддержки биндингов для IBlockMeta. */
trait BlockMetaBindable {

  implicit def blockMetaQsb(implicit intB: QueryStringBindable[Int],
                            boolB: QueryStringBindable[Boolean]): QueryStringBindable[IBlockMeta] = {
    new QueryStringBindable[IBlockMeta] {
      def WIDTH_SUF     = ".a"
      def HEIGHT_SUF    = ".b"
      def BLOCK_ID_SUF  = ".c"
      def IS_WIDE_SUF   = ".d"

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, IBlockMeta]] = {
        for {
          maybeWidth    <- intB.bind(key + WIDTH_SUF, params)
          maybeHeight   <- intB.bind(key + HEIGHT_SUF, params)
          maybeBlockId  <- intB.bind(key + BLOCK_ID_SUF, params)
          maybeIsWide   <- boolB.bind(key + IS_WIDE_SUF, params)
        } yield {
          for {
            width   <- maybeWidth.right
            height  <- maybeHeight.right
            blockId <- maybeBlockId.right
            isWide  <- maybeIsWide.right
          } yield {
            BlockMeta(
              width   = width,
              height  = height,
              blockId = blockId,
              wide    = isWide
            )
          }
        }
      }

      override def unbind(key: String, value: IBlockMeta): String = {
        Iterator(
          intB.unbind(key + WIDTH_SUF,    value.width),
          intB.unbind(key + HEIGHT_SUF,   value.height),
          intB.unbind(key + BLOCK_ID_SUF, value.blockId),
          boolB.unbind(key + IS_WIDE_SUF, value.wide)
        )
          .filter(!_.isEmpty)
          .mkString("&")
      }
    }
  }

}

