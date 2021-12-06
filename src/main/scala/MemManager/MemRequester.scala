package MemManager

import scala.collection.mutable

abstract class MemRequester(init_addr: Int, word_count: Int) {
  private var acc_addr = init_addr
  private val addr_range_record = mutable.Map.empty[Range, String]
  private val countWordNum = (d: Int) => Math.ceil(d.toDouble / word_count).toInt
}
