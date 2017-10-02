package io.suggest.crypto.asm

import com.github.vibornoff.asmcryptojs.AsmCrypto
import io.suggest.crypto.hash.{MHash, MHashes}
import io.suggest.pick.JsBinaryUtil
import io.suggest.ww.IWwTask
import org.scalajs.dom.Blob
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 11:23
  * Description: WebWorker задача, выполняющая хэширование указанным алгоритмом.
  */
case class HashWwTask(
                       hash: MHash,
                       blob: Blob
                     )
  extends IWwTask[String] {

  override def run() = {
    JsBinaryUtil
      .blob2arrBuf(blob)
      .map { data =>
        val hasher = hash match {
          case MHashes.Sha1     => AsmCrypto.SHA1
          case MHashes.Sha256   => AsmCrypto.SHA256
        }
        hasher.hex(data)
      }
  }

}

