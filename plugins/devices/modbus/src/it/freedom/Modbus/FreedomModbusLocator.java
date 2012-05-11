/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.freedom.Modbus;

import com.serotonin.modbus4j.BatchRead;
import com.serotonin.modbus4j.BatchResults;
import com.serotonin.modbus4j.ModbusLocator;
import com.serotonin.modbus4j.code.RegisterRange;
import it.freedomotic.events.GenericEvent;
import it.freedomotic.model.ds.Config;

/**
 *  This class is used to encapsulate the ModbusLocator used by Modbus4J and to
 * extend with Freedom specific functionality.
 * The locators are the elements that are used to configure the reads /writes on
 * the modbus and to expose that information in Freedom.
 * @author gpt
 */
class FreedomModbusLocator {

    private ModbusLocator modbusLocator;
    private String name;
    private int slaveId;
    private int registerRange;
    private int dataType;
    private int offset;
    //private int numberOfRegisters;
    private Byte bit;
    //private String characterEncoding;
    private Double multiplier;
    private Double additive;
    private String eventName;

    /**
     *  Constructor.
     * @param configuration
     * @param i
     */
    FreedomModbusLocator(Config configuration, int i) {

            name = configuration.getTuples().getStringProperty(i,"Name","DefaultName");
            slaveId = configuration.getTuples().getIntProperty(i,"SlaveId",0);
            registerRange = parseRegisterRange(configuration.getTuples().getStringProperty(i,"RegisterRange","COIL_STATUS"));
            dataType = parseDataType(configuration.getTuples().getStringProperty(i,"DataType","BINARY"));
            offset = configuration.getTuples().getIntProperty(i,"Offset",0);


            //TODO: The Modbus4j functionality must be extend to allow to read and combine several "bit" values from
            // the same register. For example bit1&bit2 generates a 4 four states value and there are Slaves that uses this format,
            // and should be abstracted.

            //we try to parse the bit value.
            bit = Byte.valueOf(configuration.getTuples().getStringProperty(i,"Bit","-1"));
            if (bit!=-1)
            {
                if (registerRange != RegisterRange.HOLDING_REGISTER &&
                    registerRange != RegisterRange.INPUT_REGISTER)
                {
                //throw a bad configuration exception.
                }
                else
                {
                    modbusLocator = new ModbusLocator(slaveId,registerRange,offset,dataType,bit);
                }
            }
            else
            {
                modbusLocator = new ModbusLocator(slaveId,registerRange,offset,dataType);
            }
            //TODO: use the number of registers
            //numberOfRegisters= configuration.getTuples().getIntProperty(i,"NumberOfRegisters",1);
            //At this moment the characterEncoding is not necesary
            //characterEncoding=configuration.getTuples().getStringProperty(i,"CharacterEncoding","ASCII");         
            multiplier=configuration.getTuples().getDoubleProperty(i,"Multiplier",1);
            additive=configuration.getTuples().getDoubleProperty(i,"Additive",0);            
            eventName=configuration.getTuples().getStringProperty(i,"EventName","Event");

    }

    private int parseRegisterRange(String stringProperty) {

        //TODO: Check that the RegisterRange is correct
        //TODO: use an enum?
        // Admited values: CoilStatus, InputStatus, HoldingRegister, InputRegister
        if (stringProperty.equals("COIL_STATUS"))
            return 1;
        else if(stringProperty.equals("INPUT_STATUS")) 
            return 2;
        else if(stringProperty.equals("HOLDING_REGISTER")) 
             return 3;
        else if(stringProperty.equals("INPUT_REGISTER")) 
            return 4;       
        else
            return -1; //TODO: Handle format error
    }

    private int parseDataType(String stringProperty) {

        if (stringProperty.equals("BINARY"))
            return 1;
        else if(stringProperty.equals("TWO_BYTE_INT_UNSIGNED"))
            return 2;
        else if(stringProperty.equals("TWO_BYTE_INT_SIGNED"))
             return 3;
        else if(stringProperty.equals("FOUR_BYTE_INT_UNSIGNED"))
            return 4;
        else if(stringProperty.equals("FOUR_BYTE_INT_SIGNED"))
            return 5;
        else if(stringProperty.equals("FOUR_BYTE_INT_UNSIGNED_SWAPPED"))
            return 6;
        else if(stringProperty.equals("FOUR_BYTE_INT_SIGNED_SWAPPED"))
            return 7;
        else if(stringProperty.equals("FOUR_BYTE_FLOAT"))
            return 8;
        else if(stringProperty.equals("FOUR_BYTE_FLOAT_SWAPPED"))
            return 9;
        else if(stringProperty.equals("EIGHT_BYTE_INT_UNSIGNED"))
            return 10;
        else if(stringProperty.equals("EIGHT_BYTE_INT_SIGNED"))
            return 11;
        else if(stringProperty.equals("EIGHT_BYTE_INT_UNSIGNED_SWAPPED"))
            return 12;
        else if(stringProperty.equals("EIGHT_BYTE_INT_SIGNED_SWAPPED"))
            return 13;
        else if(stringProperty.equals("EIGHT_BYTE_FLOAT"))
            return 14;
        else if(stringProperty.equals("EIGHT_BYTE_FLOAT_SWAPPED"))
            return 15;
        else if(stringProperty.equals("TWO_BYTE_BCD"))
            return 16;
        else if(stringProperty.equals("FOUR_BYTE_BCD"))
            return 17;
        else
            return -1; //TODO: Handle format error
    }

    /**
     * Fills the BatchRead with the information of the ModbusLocators
     * @param batchRead the batchRead that is going to be updated
     */
    void updateBatchRead(BatchRead<String> batchRead) {
        //TODO: The Name is not a good id.
        batchRead.addLocator(name, getModbusLocator());
    }

    /**
     * Uses the FreedomModbusLocator to fill an Event with the correct information
     * Used by the ModbusSensor
     * @param results the readed values
     * @param event The event that is filled with the information
     */
    void fillEvent(BatchResults<String> results, GenericEvent event) {
        //GenericEvent event = new GenericEvent(sensor);
        //TODO: We can use a switch over the eventName to send a more specialiced event               
        String value;
        if (bit!=-1) //it's a bit value
            value=results.getValue(name).toString();
        else //it's a numeric value
            value = Double.toString(getAdjustedValue(Double.parseDouble((results.getValue(name).toString()))));

        event.addProperty(eventName,  value);
    }

    /**
     * @return the modbusLocator
     */
    protected ModbusLocator getModbusLocator() {
        return modbusLocator;
    }
    /**
     *  Transforms the value using the FreedomModbusLocator information to translate
     * from / to freedom to modbus scales
     * @param value the value to transform
     * @return the transformed value
     */
    private double getAdjustedValue(double value)
    {
        return value*multiplier+additive;
    }
 
    Object parseValue(Config properties, int i) {
        //TODO: use the DataType to parse the correct type
        String value = properties.getTuples().getStringProperty(0,"value","0");
        if (bit!=-1)
            return value;
        else
            return getAdjustedValue(Double.parseDouble(value));
    }



}