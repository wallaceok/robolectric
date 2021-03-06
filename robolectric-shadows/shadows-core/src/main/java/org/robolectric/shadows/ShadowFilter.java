package org.robolectric.shadows;

import android.widget.Filter;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

@Implements(Filter.class)
public class ShadowFilter {
  @RealObject private Filter realObject;

  @Implementation
  public void filter(CharSequence constraint, Filter.FilterListener listener) {
    try {
      Class<?> forName = Class.forName("android.widget.Filter$FilterResults");
      Object filtering = ReflectionHelpers.callInstanceMethod(realObject, "performFiltering",
          ClassParameter.from(CharSequence.class, constraint));

      ReflectionHelpers.callInstanceMethod(realObject, "publishResults",
          ClassParameter.from(CharSequence.class, constraint),
          ClassParameter.from(forName, filtering));

    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Cannot load android.widget.Filter$FilterResults");
    }
  }
}
