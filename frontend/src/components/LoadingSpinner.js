import React from 'react';
import './LoadingSpinner.css';

const LoadingSpinner = ({ message = 'Loading...' }) => {
  return (
    <div className="spinner-container">
      <div className="spinner-ring">
        <div className="spinner-ring-inner" />
      </div>
      <p className="spinner-text">{message}</p>
    </div>
  );
};

export default LoadingSpinner;
