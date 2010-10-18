package com.atlassian.fage.functions;


import org.mockito.Mock;
import org.slf4j.Logger;

import junit.framework.TestCase;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class TestExceptionActions extends TestCase
{
    @Mock private Logger log;
    @Mock private Exception exception;
    
    public void setUp()
    {
        initMocks(this);
    }
    
    public void testLoggingExceptionAction()
    {
        ExceptionAction loggingExceptionAction = ExceptionActions.loggingExceptionAction(log);
        loggingExceptionAction.act(exception);
        
        verify(log).warn("Exception encountered: ", exception);
    }
    
    public void testLoggingDelayingExceptionAction()
    {
        ExceptionAction loggingDelayingExceptionAction = ExceptionActions.chain(ExceptionActions.loggingExceptionAction(log), ExceptionActions.delayingExceptionAction(100));
        long startTime = System.currentTimeMillis();
        
        loggingDelayingExceptionAction.act(exception);
        
        long actualTime = System.currentTimeMillis() - startTime;
        
        assertTrue(actualTime >= 100);
        verify(log).warn("Exception encountered: ", exception);
    }
    
    
}
