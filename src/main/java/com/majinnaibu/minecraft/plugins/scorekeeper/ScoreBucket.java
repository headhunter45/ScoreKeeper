/*
This file is part of ScoreKeeper.

ScoreKeeper is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

ScoreKeeper is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with ScoreKeeper. If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
*/

package com.majinnaibu.minecraft.plugins.scorekeeper;

public final class ScoreBucket {
  private final String _id;
  private final String _singular;
  private final String _plural;
  private final int _initialValue;

  public ScoreBucket(String id, String singular, String plural, int initialValue) {
    if (id == null || !id.matches("[A-Za-z0-9_-]+")) {
      throw new IllegalArgumentException("Bucket ID must contain only letters, numbers, _ or -");
    }
    if (singular == null || singular.isBlank()) {
      throw new IllegalArgumentException("Bucket singular label cannot be blank");
    }
    if (plural == null || plural.isBlank()) {
      throw new IllegalArgumentException("Bucket plural label cannot be blank");
    }
    _id = id;
    _singular = singular;
    _plural = plural;
    _initialValue = initialValue;
  }

  public String getId() {
    return _id;
  }

  public String getSingular() {
    return _singular;
  }

  public String getPlural() {
    return _plural;
  }

  public int getInitialValue() {
    return _initialValue;
  }
}
