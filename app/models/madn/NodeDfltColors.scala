package models.madn

import io.suggest.model.EnumValue2Val

import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.15 11:58
 * Description: Статическая модель с дефолтовыми цветами оформления новосоздаваемых узлов.
 */
object NodeDfltColors extends Enumeration(0) with EnumValue2Val {

  /**
   * Внутренняя проверка на валидность указания цвета.
   * @param color hex-код цвета RGB.
   */
  private def assertColorValid(color: String): Unit = {
    // TODO Использовать регэксп
    assert(color.length == 6 && !color.startsWith("#"), s"invalid color code: $color")
  }

  /**
   * Экземпляр модели дефолтового цвета.
   * @param bgColor Цвет фона.
   * @param fgColor Цвет текста/элементов.
   */
  protected sealed class Val(val bgColor: String, val fgColor: String) extends super.Val {
    assertColorValid(bgColor)
    assertColorValid(fgColor)
  }

  override type T = Val

  val C1  : T = new Val("544d4d", "f0eded")
  val C2  : T = new Val("774b5d", "ffffff")
  val C3  : T = new Val("243446", "c9dbc4")
  val C4  : T = new Val("2f353e", "d5e0f8")
  val C5  : T = new Val("aabfb0", "7e2d3e")
  val C6  : T = new Val("def6e8", "454849")
  val C7  : T = new Val("cf5048", "eefaf9")
  val C8  : T = new Val("b5b7b6", "39445d")
  val C9  : T = new Val("e7e7e7", "6c849e")
  val C10 : T = new Val("9f9295", "952f48")

  /** Вернуть рандомный цвет. */
  def getOneRandom(rnd: Random = new Random()): T = {
    val id = rnd.nextInt(values.size)
    apply(id)
  }

}


