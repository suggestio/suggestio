package io.suggest.ym.model.common

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model._
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel.FieldsJsonAcc
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{FilterBuilder, FilterBuilders, QueryBuilders, QueryBuilder}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.14 18:54
 * Description: Аддон для поддержки поля LogoImg, хранящий json object по логотипу с метаданными.
 */

object EMLogoImg {
  val LOGO_IMG_ESFN = "logoImg"

  def esMappingField = FieldObject(LOGO_IMG_ESFN, enabled = false, properties = Nil)

  /** Скрипт для фильтрации по наличию значения в поле logo. */
  def LOGO_EXIST_MVEL = {
    val fn = EMLogoImg.LOGO_IMG_ESFN
    s"""_source.containsKey("$fn");"""
  }

}


import EMLogoImg._


trait EMLogoImgStatic extends EsModelStaticMutAkvT {
  override type T <: EMLogoImgMut

  abstract override def generateMappingProps: List[DocField] = {
    esMappingField :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (LOGO_IMG_ESFN, value)  =>
        acc.logoImgOpt = Option(MImgInfo.convertFrom(value))
    }
  }
}


trait LogoImgOptI {
  def logoImgOpt: Option[MImgInfoT]
}

trait EMLogoImgI extends EsModelPlayJsonT with LogoImgOptI {
  override type T <: EMLogoImgI
}

trait EMLogoImg extends EMLogoImgI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (logoImgOpt.isDefined)
      LOGO_IMG_ESFN -> logoImgOpt.get.toPlayJson :: acc0
    else
      acc0
  }

  override def doEraseResources(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    val fut = super.doEraseResources
    logoImgOpt.fold(fut) { img =>
      MImg2Util.deleteFully(img.filename)
        .flatMap { _ => fut }
    }
  }
}

trait EMLogoImgMut extends EMLogoImg {
  override type T <: EMLogoImgMut
  var logoImgOpt: Option[MImgInfoT]
}



// DynSearch-аддоны
trait LogoImgExistsDsa extends DynSearchArgs {

  /** Фильтровать по наличию/отсутсвию логотипа. */
  def hasLogo: Option[Boolean]

  /** Собрать экземпляр ES QueryBuilder на основе имеющихся в экземпляре данных.
    * Здесь можно навешивать дополнительные фильтры, выполнять post-процессинг запроса. */
  override def toEsQuery: QueryBuilder = {
    var qb2 = super.toEsQuery
    // Добавить фильтр по наличию логотипа. Т.к. поле не индексируется, то используется
    if (hasLogo.nonEmpty) {
      var ef: FilterBuilder = FilterBuilders.scriptFilter(LOGO_EXIST_MVEL).lang("mvel")
      if (!hasLogo.get) {
        ef = FilterBuilders.notFilter(ef)
      }
      qb2 = QueryBuilders.filteredQuery(qb2, ef)
    }
    qb2
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    val sis = super.sbInitSize
    if (hasLogo.isDefined)  sis + 16  else  sis
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("hasLogo", hasLogo, super.toStringBuilder)
  }
}

trait LogoImgExistsDsaDflt extends LogoImgExistsDsa {
  override def hasLogo: Option[Boolean] = None
}

trait LogoImgExistsDsaWrapper extends LogoImgExistsDsa with DynSearchArgsWrapper {
  override type WT <: LogoImgExistsDsa
  override def hasLogo = _dsArgsUnderlying.hasLogo
}

