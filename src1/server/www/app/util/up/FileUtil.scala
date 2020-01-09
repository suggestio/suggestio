package util.up

import java.io.{File, FileInputStream}
import javax.inject.Inject

import io.suggest.crypto.hash.{MHash, MHashes}
import io.suggest.model.n2.media.MFileMetaHash
import io.suggest.util.logs.MacroLogsImpl
import org.apache.commons.codec.digest.DigestUtils
import scala.concurrent.blocking

import scala.concurrent.{ExecutionContext, Future}

// TODO Унести в [util] или ещё куда-нибудь. Нет необходимости держать это в [www].

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 11:49
 * Description: Утиль для работы с файлами в файловой системе.
 */
class FileUtil @Inject()(
                          implicit private val ec: ExecutionContext
                        )
  extends MacroLogsImpl
{

  /**
    * Рассчитать SHA-1 для файла.
    *
    * @param file Исходный файл.
    * @return Строка чексуммы вида "bf35fa420d3e0f669e27b337062bf19f510480d4".
    * @see Написано по мотивам [[http://stackoverflow.com/a/2932513]].
    */
  def sha1(file: File): String = {
    _hashStream( file )( DigestUtils.sha1Hex )
  }

  def sha256(file: File): String = {
    _hashStream( file )( DigestUtils.sha256Hex )
  }

  def mkFileHash(mhash: MHash, file: File): String = {
    mhash match {
      case MHashes.Sha1   => sha1(file)
      case MHashes.Sha256 => sha256(file)
    }
  }

  // TODO Удалить этот код следом за заливкой 1-го поколения.
  def mkHashesHexAsync(file: File, hashes: Iterable[MHash], flags: Set[Short]): Future[Seq[MFileMetaHash]] = {
    val futs = Future.traverse(hashes) { mhash =>
      Future {
        MFileMetaHash(
          hType     = mhash,
          hexValue  = mkFileHash(mhash, file),
          flags     = flags
        )
      }
    }
    for (kvs <- futs) yield
      kvs.toSeq
  }

  private def _hashStream(file: File)(f: FileInputStream => String): String = {
    blocking {
      val is = new FileInputStream(file)
      try {
        f(is)
      } finally {
        is.close()
      }
    }
  }

}
