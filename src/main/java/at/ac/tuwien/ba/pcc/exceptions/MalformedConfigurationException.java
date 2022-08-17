package at.ac.tuwien.ba.pcc.exceptions;

public class MalformedConfigurationException extends RuntimeException{
    public MalformedConfigurationException(String msg, Throwable cause){
        super(msg, cause);
    }
}
