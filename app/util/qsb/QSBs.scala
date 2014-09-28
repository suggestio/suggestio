package util.qsb

import play.api.mvc.QueryStringBindable
import models._

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


object QSBs {

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


  /** routes-биндер для MImgInfoMeta, которая содержит размеры. */
  implicit def mImgInfoMetaQsb(implicit intB: QueryStringBindable[Int]) = {
    new QueryStringBindable[MImgInfoMeta] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MImgInfoMeta]] = {
        for {
          maybeWidth  <- intB.bind(key + ".w", params)
          maybeHeight <- intB.bind(key + ".h", params)
        } yield {
          maybeWidth.right.flatMap { width =>
            maybeHeight.right.map { height =>
              MImgInfoMeta(width = width, height = height)
            }
          }
        }
      }

      override def unbind(key: String, value: MImgInfoMeta): String = {
        s"$key.w=${value.width}&$key.h=${value.height}"
      }
    }
  }

}


