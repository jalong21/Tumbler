package services

import scala.util.Using
import redis.clients.jedis.{Jedis, JedisPool}

object RedisService {

  // This is assuming you have a redis instance running at this port locally
  // run "docker run -d --name redis-stack-server -p 6379:6379 redis/redis-stack-server:latest"
  val jedisPool = new JedisPool("localhost", 6379)

  def getValue(key: String)  =
    Using(jedisPool.getResource) { jedis => jedis.get(key)}

  def setValue(key: String, value: String) =
    Using(jedisPool.getResource) { jedis => jedis.set(key, value)}
}
