package io.suggest.adv

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.01.16 14:34
  * Description: Констатны, общие для разных ADV-форм.
  */
object AdvConstants {

  /** Константы для ценника. */
  object Price {

    /** id элемента-контейнера, в котором отображается рассчитанная цена размещния.
      * Изначально это был тэг P. */
    def PRICE_INFO_CONT_ID = "apic"

  }


  /** Константы adv-форм для суперюзеров s.io. */
  object Su {

    def PREFIX            = "sua"

    /** id чекбокса, содержащего привелегированную галочку бесплатного размещения карточки. */
    def ADV_FOR_FREE_ID   = PREFIX + "fcb"

    /** Имя поля галочки бесплатного размещения, сабмиттящееся на сервер. */
    def ADV_FOR_FREE_NAME = "freeAdv"

  }

}
