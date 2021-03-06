/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.cache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 *
 * 最少使用缓存，运用 LinkedHashMap 巧妙设计
 * Lru (least recently used) cache decorator
 *
 * @author Clinton Begin
 */
public class LruCache implements Cache {

  private final Cache delegate;
  // 记录key最近的使用情况
  private Map<Object, Object> keyMap;
  // 记录最少被使用的缓存项key
  private Object eldestKey;

  public LruCache(Cache delegate) {
    this.delegate = delegate;
    setSize(1024);
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  // 重新设置缓存大小时，会重置keyMap字段
  public void setSize(final int size) {
    // 有序HashMap，accessOrder说明了 LinkedHashMap 的 get()会改变其记录的顺序
    keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
      private static final long serialVersionUID = 4267176411845948333L;

      // 当调用LinkedHashMap 的 put() 方法时，会调用该方法
      // 重写了LinkedHashMap 的方法，如果map的size大于参数size 就将最老的元素赋值给eldestKey
      // accessOrder 为true，说明插入无序，读取有序，最先读取的排在最前面，从而达到最少使用的最先去掉
      @Override
      protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
        boolean tooBig = size() > size;
        if (tooBig) {
          // 如果已达到缓存的上限，则更新eldestKey字段，后面会删除该项
          eldestKey = eldest.getKey();
        }
        return tooBig;
      }
    };
  }

  @Override
  public void putObject(Object key, Object value) {
    // 添加缓存项
    delegate.putObject(key, value);
    // 去除最少使用的entry
    cycleKeyList(key);
  }

  @Override
  public Object getObject(Object key) {
    // touch 修改LinkedHashMap中记录的顺序
    keyMap.get(key);
    return delegate.getObject(key);
  }

  @Override
  public Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  @Override
  public void clear() {
    delegate.clear();
    keyMap.clear();
  }

  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  // 去除最少使用的entry
  private void cycleKeyList(Object key) {
    keyMap.put(key, key);
    // 这个地方 eldestKey 会在 LinkedHashMap 中的 removeEldestEntry()设置，
    // removeEldestEntry() 应该是钩子方法，在 LinkedHashMap的恰当时机会被调用
    if (eldestKey != null) {
      delegate.removeObject(eldestKey);
      eldestKey = null;
    }
  }

}
