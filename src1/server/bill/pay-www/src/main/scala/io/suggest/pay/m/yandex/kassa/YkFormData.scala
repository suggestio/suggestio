package io.suggest.pay.m.yandex.kassa

import io.suggest.mbill2.m.gid.Gid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.02.17 21:27
  * Description: Модель данных формы запуска платежа в яндекс-кассе.
  * Это данные для самого-самого первого шага.
  */

/**
  * Класс модели данных, передаваемых в форме для яндекс кассы.
  *
  * @param shopId id магазина (из конфига).
  * @param scId id витрины магазина (из конфига).
  * @param customerId personId
  * @param sum Сумма платежа (в рублях).
  * @param userEmail Email пользователя, если известен.
  * @param orderNumber Номер заказа (MOrders).
  * @param orderData Сериализованные и подписанные ключиком данные заказа для защиты от измений во время оплаты.
  */
case class YkFormData(
                       shopId       : Int,
                       scId         : Int,
                       customerId   : String,
                       sum          : Double,
                       userEmail    : Option[String],
                       orderNumber  : Gid_t,
                       orderData    : String
                     )
