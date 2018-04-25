package util.img

import io.suggest.img.SioImageUtilT
import io.suggest.util.logs.MacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 15:14
 * Description: Для работы с загружаемыми картинками используются эти вспомогательные функции.
 */


// TODO OrigImageUtilk -- очень древний компонент, но он ещё используется. Может быть сделать, чтобы его не было вообще? Там по сути только вызов convert внутри.

/** Резайзилка картинок, используемая для генерация "оригиналов", т.е. картинок, которые затем будут кадрироваться. */
class OrigImageUtil
  extends SioImageUtilT
  with MacroLogsImpl
{

  /** Качество сжатия jpeg. */
  override def JPEG_QUALITY_PC: Double = 90

  override def GAUSSIAN_BLUG = None

}

/** Интерфейс к DI-полю с инстансом [[OrigImageUtil]]. */
trait IOrigImageUtilDi {
  def origImageUtil: OrigImageUtil
}

