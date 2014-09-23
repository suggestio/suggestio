package models

import io.suggest.ym.model.common.LogoImgOptI
import play.api.mvc.QueryStringBindable
import util.qsb.QsbUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.08.14 14:22
 * Description: Контейнеры для аргументов, передаваемых в компоненты showcase.
 */
object SMShowcaseReqArgs {

  /** routes-Биндер для параметров showcase'а. */
  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]]) = {
    new QueryStringBindable[SMShowcaseReqArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, SMShowcaseReqArgs]] = {
        for {
          maybeGeo  <- strOptB.bind(key + ".geo", params)
        } yield {
          Right(SMShowcaseReqArgs(
            geo  = GeoMode.maybeApply(maybeGeo).filter(_.isWithGeo).getOrElse(GeoIp)
          ))
        }
      }

      override def unbind(key: String, value: SMShowcaseReqArgs): String = {
        strOptB.unbind(key + ".geo", value.geo.toQsStringOpt)
      }
    }
  }

}

case class SMShowcaseReqArgs(
  geo: GeoMode = GeoNone
) {
  override def toString: String = s"${geo.toQsStringOpt.map { "geo=" + _ }}"
}




/**
 * Аргументы для рендера market/showcase/indexTpl.
 * Экстендим LogoImgOptI чтобы компилятор был в курсе изменений API полей логотипов в sioutil.
 * @param bgColor Используемый цвет выдачи.
 * @param mmcats Категории для отображения.
 * @param catsStats Статистика по категориям.
 * @param spsr Поисковый запрос.
 * @param oncloseHref Абсолютный URL перехода при закрытии выдачи.
 * @param logoImgOpt Логотип, если есть.
 * @param shops Список магазинов в торговом центре.
 * @param welcomeAdOpt Приветствие, если есть.
 */
case class SMShowcaseRenderArgs(
  bgColor: String,
  fgColor: String,
  name: String,
  mmcats: Seq[MMartCategory],
  catsStats: Map[String, Long],
  spsr: AdSearch,
  oncloseHref: String,
  geoListGoBack: Option[Boolean] = None,
  logoImgOpt: Option[MImgInfoT] = None,
  shops: Map[String, MAdnNode] = Map.empty,
  welcomeAdOpt: Option[MWelcomeAd] = None
) extends LogoImgOptI

