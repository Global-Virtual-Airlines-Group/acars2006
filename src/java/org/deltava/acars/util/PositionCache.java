// Copyright 2005 Luke J. Kolin. All Rights Reserved.
package org.deltava.acars.util;

import java.util.*;

import org.deltava.acars.message.PositionMessage;

/**
 * A utility class to cache position messages.
 * @author Luke
 * @version 1.0
 * @since 1.0
 */

public final class PositionCache {

   private static Set _cache = new LinkedHashSet();
   private static long _lastFlush;
   
   public static class PositionCacheEntry {

      private PositionMessage _msg;
      private long _conID;
      private int _flightID;
      
      private PositionCacheEntry(PositionMessage msg, long cID, int fID) {
         super();
         _msg = msg;
         _conID = cID;
         _flightID = fID;
      }
      
      public PositionMessage getMessage() {
         return _msg;
      }
      
      public long getConnectionID() {
         return _conID;
      }
      
      public int getFlightID() {
         return _flightID;
      }
      
      public boolean equals(Object o) {
         PositionCacheEntry e2 = (PositionCacheEntry) o;
         if (_conID != e2._conID)
            return false;
         
         return (_msg.getID() == e2._msg.getID());
      }
   }
   
   // singleton constructor
   private PositionCache() {
   }
   
   /**
    * Adds a new position entry to the cache.
    * @param msg the Position data
    * @param conID the ACARS connection ID
    * @param flightID the ACARS flight ID
    */
   public static synchronized void push(PositionMessage msg, long conID, int flightID) {
      _cache.add(new PositionCacheEntry(msg, conID, flightID));
   }
   
   /**
    * Returns all of the position cache entries.
    * @return a Collection of PositionCacheEntry beans
    */
   public static Collection getAll() {
      return _cache;
   }
   
   /**
    * Returns the number of milliseconds since the last position flush
    * @return
    */
   public static long getFlushInterval() {
      return System.currentTimeMillis() - _lastFlush;
   }
   
   /**
    * Returns if the cache contains any entries.
    * @return TRUE if the cache is not empty, otherwise FALSE
    * @see Collection#isEmpty()
    */
   public static boolean isDirty() {
      return (!_cache.isEmpty());
   }
   
   /**
    * Clears the position cache.
    */
   public static void flush() {
      _cache.clear();
      _lastFlush = System.currentTimeMillis();
   }
}