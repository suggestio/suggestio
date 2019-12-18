package io.suggest.fio

import java.io.File

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 22:36
 * Description: Модель аргументов запроса сохранения файла в абстрактное хранилище.
 * @param contentType Тип данных в файле.
 * @param file Файл в файловой системе.
 * @param origFileName Оригинальное имя файла, если есть.
 */
case class WriteRequest(
                         contentType  : String,
                         file         : File,
                         origFileName : Option[String]  = None,
                       )
