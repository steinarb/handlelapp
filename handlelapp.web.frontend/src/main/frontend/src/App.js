import React, { Component } from 'react';
import { Switch, Route } from 'react-router-dom';
import { ConnectedRouter as Router } from 'connected-react-router';
import './App.css';
import Home from './components/Home';
import Login from './components/Login';
import Unauthorized from './components/Unauthorized';

class App extends Component {
    render() {
        const { history } = this.props;

        return (
            <Router history={history}>
                <Switch>
                    <Route exact path="/handlelapp/" component={Home} />
                    <Route exact path="/handlelapp/login" component={Login} />
                    <Route exact path="/handlelapp/unauthorized" component={Unauthorized} />
                </Switch>
            </Router>
        );
    }
}

export default App;
