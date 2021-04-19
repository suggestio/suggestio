package io.suggest.jd

import io.suggest.ad.blk.{BlockPaddings, BlockWidths}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.10.17 14:37
  * Description: Константы jd-подсистемы.
  */
object JdConst {

  /** Макс.кол-во стрипов в одном документе. */
  final def MAX_STRIPS_COUNT = 50

  /** Макс.кол-во элементов в минимальном блоке.
    * 2 шт - было мало. Увелично до 5 на один квадратик. */
  final def MAX_ELEMENTS_PER_MIN_BLOCK = 5


  /** Макс. кол-во qd-операций в одном "контенте". */
  final def MAX_QD_OPS_COUNT = 200


  final def MAX_TEXT_LEN = 4096

  /** Максимально-допустимое вращение jd-элемента. */
  final def ROTATE_MAX_ABS = 180


  object Shadow {
    object TextShadow {
      final def HORIZ_OFFSET_MIN_MAX = 30
      final def VERT_OFFSET_MIN_MAX = 50

      /** На сколько делить/умножать хранимое значение.
        * Для упрощения, дробные пиксели хранятся в целых числах. */
      final def BLUR_FRAC = 10
      /** Максимальная размывка в пикселях экрана. */
      final def BLUR_MAX = 5
    }
  }

  object ContentWidth {
    def MIN_PX = BlockPaddings.default.fullBetweenBlocksPx
    def MAX_PX = 4 * BlockWidths.NORMAL.value
  }


  object Event {

    /** Макс. кол-во обработчиков событий на один тег. */
    def MAX_EVENT_LISTENERS_PER_TAG = 1

    def MAX_ACTIONS_PER_LISTENER = 1

    /** Макс.число карточек в одном экшене. */
    def MAX_ADS_PER_ACTION = 10

  }

}
