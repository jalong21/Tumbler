package Utils

import net.sf.ehcache.CacheManager

object TumbleCache {

  private val cacheManager = CacheManager.getInstance()
  cacheManager.addCacheIfAbsent("TumbleCache")
  private val cache = cacheManager.getCache("TumbleCache")

  def getCache = cache
}
