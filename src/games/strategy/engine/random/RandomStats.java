/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package games.strategy.engine.random;

import games.strategy.engine.message.IDestination;
import games.strategy.engine.message.IMessageManager;
import games.strategy.engine.message.Message;
import games.strategy.util.IntegerMap;



public class RandomStats implements IDestination
{
    
    public static String RANDOM_STATS_DESTINATION = "__RandomStatsSource__";
    private IntegerMap m_randomStats = new IntegerMap();
    
    public RandomStats(IMessageManager manager)
    {
        manager.addDestination(this);
    }
    
    public synchronized void addRandom(int[] random)
    {
        for(int i = 0; i < random.length; i++)
        {
            m_randomStats.add(new Integer(random[i] + 1), 1 );
        }
    }

    public synchronized void addRandom(int random)
    {
        m_randomStats.add(new Integer(random + 1), 1 );
    }

    public synchronized Message sendMessage(Message message)
    {
        return new RandomStatsMessage(m_randomStats);
    }

    public String getName()
    {
        return RANDOM_STATS_DESTINATION;
    }
    
    
    
    
    
}
