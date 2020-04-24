package io.suggest.daemon

import io.suggest.color.MColorData
import io.suggest.spa.DAction
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.04.2020 18:04
  * Description: Модель настроек демонизации.
  */

/** Настройки инициализации.
  *
  * @param descr Подсказки по работе демона.
  * @param events Описание событий, которые надо вбрасывать в circuit.
  * @param notification None - нагло скрывать уведомление (возможно, в нарушении рекомендаций ОС).
  *                     Some() - Реквизиты для рендера уведомления.
  */
case class MDaemonInitOpts(
                            events        : MDaemonEvents,
                            descr         : MDaemonDescr,
                            notification  : Option[MDaemonNotifyOpts],
                          )
object MDaemonInitOpts {
  @inline implicit def univEq: UnivEq[MDaemonInitOpts] = UnivEq.derive
}


/** Обработчики событий демонизации.
  *
  * @param activated Режим демона активен или деактивирован.
  * @param enabled Включение/выключение режима автоматической демонизации при сокрытии и обратно.
  * @param failure Какая-либо ошибка системы демонизации.
  */
case class MDaemonEvents(
                          activated       : Boolean => DAction,
                          enabled         : Option[Boolean => DAction] = None,
                          failure         : Option[DAction] = None,
                        )
object MDaemonEvents {
  @inline implicit def univEq: UnivEq[MDaemonEvents] = UnivEq.force
}


/** Параметры отображаемой нотификации приложения.
  * Все параметры опциональны, None подразумевает некий абстрактный дефолт.
  *
  * @param title Заголовок уведомления.
  * @param text Текст уведомления.
  * @param icon Иконка в уведомлении.
  * @param color Цвет, используемый в уведомлении.
  * @param lockScreen Отображать даже на экране блокировки? Дефолт подразумевает false.
  * @param resumeAppOnClick Открывать приложение при нажатию по уведомлению?
  * @param bigText ???
  * @param channelTitle Заголовок-название уведомления.
  * @param channelDescr Описание уведомления.
  */
case class MDaemonNotifyOpts(
                              title             : Option[String]      = None,
                              text              : Option[String]      = None,
                              icon              : Option[String]      = None,
                              color             : Option[MColorData]  = None,
                              lockScreen        : Option[Boolean]     = None,
                              resumeAppOnClick  : Option[Boolean]     = None,
                              bigText           : Option[Boolean]     = None,
                              channelTitle      : Option[String]      = None,
                              channelDescr      : Option[String]      = None,
                            )
object MDaemonNotifyOpts {
  @inline implicit def univEq: UnivEq[MDaemonNotifyOpts] = UnivEq.derive
}


/** Описание демона.
  *
  * @param needBle Требуется Bluetooth?
  * @param needGps Требуется GPS?
  */
case class MDaemonDescr(
                         needBle       : Boolean     = false,
                         needGps       : Boolean     = false,
                       )
object MDaemonDescr {
  @inline implicit def univEq: UnivEq[MDaemonDescr] = UnivEq.derive
}
