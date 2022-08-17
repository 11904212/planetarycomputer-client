package io.github11904212.pcc.exceptions;

public class MalformedConfigurationException extends RuntimeException{
    public MalformedConfigurationException(String msg, Throwable cause){
        super(msg, cause);
    }
}
