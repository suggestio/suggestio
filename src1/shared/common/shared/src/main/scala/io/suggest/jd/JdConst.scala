package io.suggest.jd

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.10.17 14:37
  * Description: Константы jd-подсистемы.
  */
object JdConst {

  /** Макс.кол-во стрипов в одном документе. */
  final def MAX_STRIPS_COUNT = 50

  /** Макс.кол-во элементов в минимальном блоке. */
  final def MAX_ELEMENTS_PER_MIN_BLOCK = 2


  /** Макс. кол-во qd-операций в одном "контенте". */
  final def MAX_QD_OPS_COUNT = 70


  final def MAX_TEXT_LEN = 1024

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

}
