package io.suggest.sc.c.dia

import diode.{ActionHandler, ActionResult, ModelRO, ModelRW}
import io.suggest.ble.beaconer.m.BtOnOff
import io.suggest.dev.MPlatformS
import io.suggest.sc.m.GeoLocOnOff
import io.suggest.sc.m.dia.first.{MWzFirstS, MWzFrames, MWzQuestions}
import io.suggest.sc.m.dia.{InitFirstRunWz, MScDialogs, ShowFirstRunWz, YesNoWz}
import io.suggest.spa.DoNothing
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.19 11:47
  * Description: Контроллер для визардов.
  */
class WizardAh[M](
                   platformRO       : ModelRO[MPlatformS],
                   modelRW          : ModelRW[M, MScDialogs],
                 )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопкам в диалоге.
    case m: YesNoWz =>
      val v0 = value
      v0.first
        .map { first0 =>
          // Рендер завершённого состояния.
          def __finish = {
            val f2 = first0.copy(
              question = MWzQuestions.Finish,
              frame    = MWzFrames.Info
            )
            Some(f2)
          }

          // На основе состояния выбрать решение
          first0.question match {

            // Идёт обсуждение вопроса на тему геолокации.
            case MWzQuestions.GeoLocPerm =>
              // Переключение на bluetooth, когда это поддерживается системой.
              def __toBt = {
                if (platformRO.value.isBleAvail) {
                  val f2 = first0.copy(
                    question = MWzQuestions.BlueToothPerm,
                    frame    = MWzFrames.AskPerm
                  )
                  Some(f2)
                } else {
                  __finish
                }
              }

              if (m.yesNo) {
                // Активировать геолокацию.
                val fx = GeoLocOnOff( enabled = true, isHard = true )
                  .toEffectPure
                val first2 = __toBt
                val v2 = v0.withFirst( first2 )
                updated(v2, fx)
              } else {
                // Отказ от геолокации.
                val first2 = first0.frame match {
                  case MWzFrames.AskPerm =>
                    val f2 = first0.withFrame( MWzFrames.Info )
                    Some(f2)
                  case MWzFrames.Info =>
                    __toBt
                }
                val v2 = v0.withFirst( first2 )
                updated(v2)
              }

            // Идёт обсуждение вопроса доступа к bluetooth.
            case MWzQuestions.BlueToothPerm =>
              if (m.yesNo) {
                val fx = BtOnOff(isEnabled = true, hard = true)
                  .toEffectPure
                val v2 = v0.withFirst( __finish )
                updated( v2, fx )
              } else {
                val f2 = first0.withFrame( MWzFrames.Info )
                val v2 = v0.withFirst( Some(f2) )
                updated(v2)
              }

            // Клик внутри окна завершения.
            case MWzQuestions.Finish =>
              // Ответ не важен - прикрыть окно диалога.
              val f2 = first0.withVisible( false )
              val v2 = v0.withFirst( Some(f2) )
              val lastFx = InitFirstRunWz(false).toEffectPure
              // TODO Какой-то эффект нужен для запуска выдачи?
              val fx = DoNothing.toEffectPure >> lastFx
              updated(v2, fx)

          }
        }
        // Нет результата.
        .getOrElse( noChange )


    // Управление фоновой инициализацией:
    case m: InitFirstRunWz =>
      val v0 = value
      if (m.isRendered && v0.first.isEmpty) {
        // Инициализировать состояние first-диалога.
        val hasGeoLoc = true
        val hasBt = platformRO.value.isBleAvail
        val first = MWzFirstS(
          visible = false,
          // В зависимости от ситуации, открыть то или иное первое окно.
          // TODO Надо всегда выдавать одинаковое приветствие с нулевым окном, а потом уже специализироваться на конкретных разрешениях.
          question = if (hasGeoLoc) MWzQuestions.GeoLocPerm
          else if (hasBt) MWzQuestions.BlueToothPerm
          else MWzQuestions.Finish,
          frame = if (hasGeoLoc || hasBt) MWzFrames.AskPerm
          else MWzFrames.Info,
        )
        val v2 = v0.withFirst( Some(first) )
        updated(v2)

      } else if (!m.isRendered && v0.first.isDefined) {
        val v2 = v0.withFirst( None )
        updated(v2)

      } else {
        noChange
      }


    // Скрыть/показать диалог.
    case m: ShowFirstRunWz =>
      val v0 = value
      v0.first
        .filter(_.visible !=* m.isShown )
        .fold(noChange) { first0 =>
          val first2 = first0.withVisible( m.isShown )
          val v2 = v0.withFirst( Some(first2) )
          updated( v2 )
        }

  }

}
