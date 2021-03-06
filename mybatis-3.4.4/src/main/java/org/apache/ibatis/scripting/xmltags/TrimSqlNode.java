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
package org.apache.ibatis.scripting.xmltags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.ibatis.session.Configuration;

/**
 *
 *TrimSqlNode 会根据子节点的解析结果，添加或者删除相应的前缀和后缀
 *
 * @author Clinton Begin
 */
public class TrimSqlNode implements SqlNode {

  // trim节点的子节点
  private SqlNode contents;
  // 记录了前缀字符串
  private String prefix;
  // 记录了后缀字符串
  private String suffix;
  // 如果包裹的SQL语句是空语句，删除指定前缀，如where
  private List<String> prefixesToOverride;
  // 如果包裹的SQL语句是空语句，删除指定的后缀，如逗号
  private List<String> suffixesToOverride;
  private Configuration configuration;

  public TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, String prefixesToOverride, String suffix, String suffixesToOverride) {
    this(configuration, contents, prefix, parseOverrides(prefixesToOverride), suffix, parseOverrides(suffixesToOverride));
  }

  protected TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, List<String> prefixesToOverride, String suffix, List<String> suffixesToOverride) {
    this.contents = contents;
    this.prefix = prefix;
    this.prefixesToOverride = prefixesToOverride;
    this.suffix = suffix;
    this.suffixesToOverride = suffixesToOverride;
    this.configuration = configuration;
  }

  @Override
  public boolean apply(DynamicContext context) {
    //包装上层次传递进来的context
    FilteredDynamicContext filteredDynamicContext = new FilteredDynamicContext(context);
    // 先调用SqlNode contents 的apply()方法
    boolean result = contents.apply(filteredDynamicContext);
    // 调用修饰者的增强方法applyAll()，处理前缀和后缀
    filteredDynamicContext.applyAll();
    return result;
  }

  // 运用 java Util StringTokenizer类，将"example1 | example2 | example3" 转化成list
  private static List<String> parseOverrides(String overrides) {
    if (overrides != null) {
      final StringTokenizer parser = new StringTokenizer(overrides, "|", false);
      final List<String> list = new ArrayList<String>(parser.countTokens());
      while (parser.hasMoreTokens()) {
        // 转换为大写，并且添加到集合中
        list.add(parser.nextToken().toUpperCase(Locale.ENGLISH));
      }
      return list;
    }
    return Collections.emptyList();
  }

  // FilteredDynamicContext 是 DynamicContext的装饰者
  private class FilteredDynamicContext extends DynamicContext {
    // 封装底层的 DynamicContext
    private DynamicContext delegate;
    // 是否已经处理过前缀和后缀，初始值都为false
    private boolean prefixApplied;
    private boolean suffixApplied;
    // 用于记录子节点解析后的结果， FilteredDynamicContext.appendSql()方法会向该字段添加解析结果，而不是调用
    // delegate..appendSql()方法
    private StringBuilder sqlBuffer;

    // 构造时，初始化必要变量
    public FilteredDynamicContext(DynamicContext delegate) {
      super(configuration, null);
      this.delegate = delegate;
      this.prefixApplied = false;
      this.suffixApplied = false;
      this.sqlBuffer = new StringBuilder();
    }

    // 增强方法 applyAll()
    public void applyAll() {
      // 获取子节点解析后的结果，并且全部转换成大写，去除掉前后空格
      sqlBuffer = new StringBuilder(sqlBuffer.toString().trim());
      // 转换成大写
      String trimmedUppercaseSql = sqlBuffer.toString().toUpperCase(Locale.ENGLISH);
      // 如果sqlBuffer 为空，不会调用applyAll() 也不会添加set
      if (trimmedUppercaseSql.length() > 0) {
        // 处理前缀
        applyPrefix(sqlBuffer, trimmedUppercaseSql);
        // 处理后缀
        applySuffix(sqlBuffer, trimmedUppercaseSql);
      }
      // 将解析后的结果添加到delegate中
      delegate.appendSql(sqlBuffer.toString());
    }

    @Override
    public Map<String, Object> getBindings() {
      return delegate.getBindings();
    }

    @Override
    public void bind(String name, Object value) {
      delegate.bind(name, value);
    }

    @Override
    public int getUniqueNumber() {
      return delegate.getUniqueNumber();
    }

    // 这个地方是关键，重写了父类的方法appendSql，将sql添加到了子类的sqlBuffer上
    @Override
    public void appendSql(String sql) {
      sqlBuffer.append(sql);
    }

    @Override
    public String getSql() {
      return delegate.getSql();
    }

    private void applyPrefix(StringBuilder sql, String trimmedUppercaseSql) {
      // 如果没有apply过
      if (!prefixApplied) {
        // apply标志位置位
        prefixApplied = true;
        // 内部类直接应用外面的变量
        if (prefixesToOverride != null) {
          // toRemove 这个名字起的很好
          for (String toRemove : prefixesToOverride) {
            // 去除开头 prefixesToOverride 里面的元素
            if (trimmedUppercaseSql.startsWith(toRemove)) {
              sql.delete(0, toRemove.trim().length());
              break;
            }
          }
        }
        // 如果前缀不为空的话，插入前缀
        if (prefix != null) {
          sql.insert(0, " ");
          sql.insert(0, prefix);
        }
      }
    }

    // 去掉后缀， eg: set name = 'wusi', 会去掉这个后面的逗号
    private void applySuffix(StringBuilder sql, String trimmedUppercaseSql) {
      if (!suffixApplied) {
        suffixApplied = true;
        if (suffixesToOverride != null) {
          for (String toRemove : suffixesToOverride) {
            if (trimmedUppercaseSql.endsWith(toRemove) || trimmedUppercaseSql.endsWith(toRemove.trim())) {
              int start = sql.length() - toRemove.trim().length();
              int end = sql.length();
              sql.delete(start, end);
              break;
            }
          }
        }
        if (suffix != null) {
          sql.append(" ");
          sql.append(suffix);
        }
      }
    }
  }
}
