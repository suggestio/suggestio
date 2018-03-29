package util.ads

import io.suggest.ads.MLkAdsOneAdAdvForm
import io.suggest.adv.decl.{MAdvDeclKey, MAdvDeclKv, MAdvDeclSpec}
import javax.inject.{Inject, Singleton}
import scalaz.{Validation, ValidationNel}
import util.adv.AdvFormUtil
import io.suggest.scalaz.ScalazUtil
import scalaz.std.iterable._
import scalaz.syntax.apply._
import scalaz.std.stream._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.03.18 18:58
  * Description: Утиль для формы управления карточками в ЛК: размещение карточек.
  */
@Singleton
class LkAdsFormUtil @Inject()(
                               advFormUtil: AdvFormUtil
                             ) {

  // TODO Надо вынести платформо-зависимое шаманство с датами и периодами размещения в typeclass IYmdHelper
  def advDeclSpecVld(spec: MAdvDeclSpec): ValidationNel[String, MAdvDeclSpec] = {
    // Проверить isShowOpened
    val isShowOpenedVld = Validation.success(spec.isShowOpened): ValidationNel[String, Option[Boolean]]

    // Проверить период размещения.
    val advPeriodVld = ScalazUtil.liftNelOpt(spec.advPeriod)(advFormUtil.advPeriodV)

    (
      isShowOpenedVld |@|
      advPeriodVld
    )(MAdvDeclSpec.apply)
  }


  /** Валидация сочетания ключа размещения и спецификации этого размещения. */
  def advDeclKvVld(declKv: MAdvDeclKv): ValidationNel[String, MAdvDeclKv] = {
    (
      MAdvDeclKey.validate(declKv.key) |@|
      advDeclSpecVld( declKv.spec )
    )(MAdvDeclKv.apply)
  }


  /** Синхронная валидация данных формы управления размещением карточки.
    *
    * @return Результат валидации.
    */
  def oneAdAdvFormVld(adForm: MLkAdsOneAdAdvForm): ValidationNel[String, MLkAdsOneAdAdvForm] = {
    ScalazUtil.validateAll( adForm.decls ) { m =>
      advDeclKvVld(m)
        .map(Stream(_))
    }
      .map(MLkAdsOneAdAdvForm(_))
  }

}
