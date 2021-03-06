/*
 * 
 * Copyright 2012 lexergen.
 * This file is part of lexergen.
 * 
 * lexergen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * lexergen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with lexergen.  If not, see <http://www.gnu.org/licenses/>.
 *  
 * lexergen:
 * A tool to chunk source code into tokens for further processing in a compiler chain.
 * 
 * Projectgroup: bi, bii
 * 
 * Authors: Johannes Dahlke
 * 
 * Module:  Softwareprojekt Übersetzerbau 2012 
 * 
 * Created: Apr. 2012 
 * Version: 1.0
 *
 */

package tokenmatcher;

import java.util.Collection;

/**
 * Modelliert einen Zustand der Finite State Machine. Ein Zustand kann als
 * Endzustand und Startzustand gekennzeichnet sein und mit einem Objekt
 * assoziert sein.
 * 
 * @author Johannes Dahlke
 * @author (Daniel Rotar)
 * 
 * @param <ConditionType>
 *            Der Typ der Bedingung für einen Zustandsübergang beim verwendeten
 *            endlichen Automaten.
 * @param <PayloadType>
 *            Der Typ des Inhalts der Zustände beim verwendeten endlichen
 *            Automaten.
 */
public interface State<ConditionType extends Comparable<ConditionType>, PayloadType> {

	/**
	 * Dient der Abfrage, ob es sich bei dem Zustand um einen Endzustand
	 * handelt.
	 * 
	 * @return true, wenn es ein Endzustand ist, sonst false
	 */
	boolean isFiniteState();

	/**
	 * Dient der Abfrage, ob es sich bei dem Zustand um einen Startzustand
	 * handelt.
	 * 
	 * @return true, wenn es ein Startzustand ist, sonst false
	 */
	boolean isInitialState();

	/**
	 * Gibt die mit dem Zustand assoziierte Ladung zurück.
	 * 
	 * @return Die mit dem Zustand assoziierte Ladung.
	 */
	PayloadType getPayload();

	/**
	 * Gibt alle Elemente, die den ausgehenden Übergängen zugeordnet sind
	 * zurück.
	 * 
	 * @return Alle Elemente, die den ausgehenden Übergängen zugeordnet sind
	 *         zurück.
	 */
	Collection<ConditionType> getElementsOfOutgoingTransitions();

}
