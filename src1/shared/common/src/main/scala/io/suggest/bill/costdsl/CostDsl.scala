package io.suggest.bill.costdsl

import io.suggest.bill.MPrice
import io.suggest.cal.m.MCalType
import io.suggest.dt.MYmd

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.04.17 22:32
  * Description: Формирование простых цен по тарифам стало чрезвычайно сложным
  * и запутанным. Особенно ярко это проявилось на попытках детализации рассчётов:
  * ничего не понятно, запутанные повторяющиеся вычисления приводят к трудно-объяснимым
  * ошибкам.
  *
  * Было решено реализовать формирование цен на базе вложенных друг в друга классов,
  * наподобии экшенов или slick-экшенов.
  *
  * + Внешний (по отношению к классам) статический компилятор и средства для
  * трансформации этих вложенных конструкций и возможность передачи всего этого
  * между клиентом и сервером.
  */

/** Базовый интерфейс-маркер для "слов" этого DSL'я. */
sealed trait ICostDslTerm


/** Базовый ценник. Используется для какой-то "отсчётной" цены.
  * На сервере, при рассчёте стоимости размещения, берётся из тарифа на текущую дату.
  */
case class BaseCost(
                     price                    : MPrice,
                     mCalType                 : Option[MCalType],
                     date                     : Option[MYmd]    // Опционален чисто на всякий случай.
                   )
  extends ICostDslTerm


/** Мультипликатор.
  * Умножение amount'а нижележащей стоимости на некоторую величину. */
case class Mult(
                 multiplifier : Double,
                 underlying   : ICostDslTerm
                 // TODO reason: IReason -- поле с пояснением. Можно тоже пачкой классов сделать.
               )

/** Сумматор.
  * Все нижележащие цены суммируются.
  * Полезно для объединения дней.
  */
case class Sum(
                children: Iterable[ICostDslTerm]
              )
